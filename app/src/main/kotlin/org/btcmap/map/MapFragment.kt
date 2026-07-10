package org.btcmap.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import org.btcmap.bundle.BundledPlaces
import org.btcmap.db.table.place.Place
import org.btcmap.place.PlaceFragment
import org.btcmap.activity.ActivityFeedFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.MapFragmentBinding
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource
import org.btcmap.area.AreaFragment
import org.btcmap.place.isMerchant
import org.btcmap.search.SearchAdapter
import org.btcmap.settings.MapStyle
import org.btcmap.settings.SettingsFragment
import org.btcmap.settings.mapStyle
import org.btcmap.settings.mapViewport
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.uri
import org.btcmap.settings.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import org.btcmap.db
import org.btcmap.sync
import org.btcmap.settings.apiUrl
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.util.openInBrowser

class MapFragment : Fragment() {
    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    var statusBarController: MapStatusBarController? = null
    var bottomSheetController: BottomSheetController? = null
    var updateNotificationController: UpdateNotificationController? = null

    private var currentCache: Any? = null
    private var mapSelectionController: MapSelectionController? = null
    private var mapSetupController: MapSetupController? = null
    private var locationController: LocationController? = null

    private lateinit var searchController: SearchController

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (it.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            ensureLocationController().onPermissionGranted(requireContext(), animateToFirstKnown = true)
        }
    }

    private fun ensureLocationController(): LocationController {
        val existing = locationController
        if (existing != null) return existing
        return LocationController(binding.map).also { locationController = it }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = MapFragmentBinding.inflate(inflater, container, false)
        initInsets(binding)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchController = SearchController(db(), resources)

        bottomSheetController = BottomSheetController(
            view = binding.placeBottomSheet,
            viewLifecycleOwner = viewLifecycleOwner,
            placeFragment = childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment,
        )

        statusBarController = MapStatusBarController(
            conf = resources.configuration,
            insetsController = WindowCompat.getInsetsController(
                requireActivity().window,
                requireActivity().window.decorView,
            ),
            bottomSheetBehavior = bottomSheetController?.bottomSheetBehavior!!,
        )
        statusBarController?.onViewCreated()

        updateNotificationController = UpdateNotificationController(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            icon = binding.update,
        )

        binding.searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_place -> openInBrowser("https://btcmap.org/add-location".toUri())
                R.id.settings -> navigateToSettings()
            }
            true
        }

        mapSetupController = MapSetupController(
            mapView = binding.map,
            styleUri = prefs.mapStyle.uri(requireContext()),
            markerBackgroundColor = prefs.markerBackgroundColor(requireContext()),
            markerBadgeBackgroundColor = prefs.badgeBackgroundColor(requireContext()),
            markerBadgeTextColor = prefs.badgeTextColor(requireContext()),
            boostedMarkerBackgroundColor = prefs.boostedMarkerBackgroundColor(),
            usingOpenFreeMap = usingOpenFreeMap(),
        ).also { it.install() }

        binding.showMerchants.setOnClickListener { setFilter(Filter.MERCHANTS) }
        binding.showEvents.setOnClickListener { setFilter(Filter.EVENTS) }
        binding.showExchanges.setOnClickListener { setFilter(Filter.EXCHANGES) }

        binding.map.getMapAsync {
            it.addOnCameraIdleListener {
                prefs.mapViewport = it.projection.visibleRegion.latLngBounds
                val center = it.projection.visibleRegion.latLngBounds.center
                viewLifecycleOwner.lifecycleScope.launch {
                    loadAreas(center.latitude, center.longitude)
                }
            }

            mapSelectionController = MapSelectionController(
                map = it,
                db = db(),
                onOpenPlace = ::selectPlace,
                onOpenEventWebsite = { url ->
                    startActivity(Intent(Intent.ACTION_VIEW, url.toString().toUri()))
                },
                onNoHit = { bottomSheetController?.hide() },
            ).also { controller -> controller.install() }
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ensureLocationController().onPermissionGranted(requireContext(), animateToFirstKnown = false)
        }

        binding.fab.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermissions()
                return@setOnClickListener
            }

            ensureLocationController().zoomToLastKnown()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.sync.isVisible = true

                val importResult = BundledPlaces.import(requireContext(), db())

                if (importResult.placesImported > 0) {
                    setFilter(filter)
                }

                val syncPlacesRes = sync().syncPlaces()
                if (syncPlacesRes.rowsAffected > 0) {
                    setFilter(filter)
                }

                if (sync().syncEvents().rowsAffected > 0 && filter == Filter.EVENTS) {
                    setFilter(filter)
                }

                val syncCommentsRes = sync().syncComments()
                if (syncCommentsRes.rowsAffected > 0 && (filter == Filter.MERCHANTS || filter == Filter.EXCHANGES)) {
                    setFilter(filter)
                }

                binding.sync.isVisible = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                binding.map.getMapAsync {
                    it.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            prefs.mapViewport, 0
                        )
                    )
                    setFilter(Filter.MERCHANTS)
                }
            }
        }

        val searchAdapter = SearchAdapter { row ->
            binding.searchView.clearText()
            binding.searchView.hide()

            val place = db().place.selectById(row.placeId) ?: return@SearchAdapter

            if (place.isMerchant()) {
                binding.showMerchants.performClick()
            } else {
                binding.showExchanges.performClick()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                selectPlace(place)
            }

            binding.map.getMapAsync {
                it.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            place.lat, place.lon
                        ), 16.0
                    )
                )
            }
        }

        binding.searchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResults.adapter = searchAdapter

        searchController.results.onEach {
            searchAdapter.submitList(it) {
                val layoutManager = binding.searchResults.layoutManager ?: return@submitList
                layoutManager.scrollToPosition(0)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        areasAdapter = AreasAdapter(apiUrl = prefs.apiUrl) { area ->
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<AreaFragment>(
                    R.id.fragmentContainerView, null, bundleOf("area_id" to area.id.toString())
                )
                addToBackStack(null)
            }
        }

        binding.areas.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            true,
        )
        binding.areas.adapter = areasAdapter

        binding.activityFeed.setOnClickListener {
            val areaIds = areasAdapter.currentList.map { it.urlAlias }
            val areaNames = areasAdapter.currentList.map { it.name }
            val areaTypes = areasAdapter.currentList.map { it.type }
            if (areaIds.isNotEmpty()) {
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<ActivityFeedFragment>(
                        R.id.fragmentContainerView, null, bundleOf(
                            "area_ids" to ArrayList(areaIds),
                            "area_names" to ArrayList(areaNames),
                            "area_types" to ArrayList(areaTypes),
                        )
                    )
                    addToBackStack(null)
                }
            }
        }

        binding.searchView.editText.doAfterTextChanged { searchString ->
            binding.map.getMapAsync { map ->
                viewLifecycleOwner.lifecycleScope.launch {
                    searchController.search(
                        referenceLocation = map.projection.visibleRegion.latLngBounds.center,
                        query = searchString.toString(),
                    )
                }
            }
        }
    }

    private fun selectPlace(place: Place) {
        val placeFragment =
            childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment
        placeFragment.setPlace(place)
        bottomSheetController?.halfExpand()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapSelectionController?.detach()
        mapSelectionController = null
        mapSetupController = null
        locationController?.destroy()
        locationController = null
        destroyCurrentCache()
        bottomSheetController = null
        statusBarController?.onDestroyView()
        statusBarController = null
        updateNotificationController = null
        _binding = null
    }

    private fun requestLocationPermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    private enum class Filter {
        MERCHANTS, EVENTS, EXCHANGES,
    }

    private var filter = Filter.MERCHANTS

    private fun setFilter(filter: Filter) {
        binding.showMerchants.isSelected = filter == Filter.MERCHANTS
        binding.showEvents.isSelected = filter == Filter.EVENTS
        binding.showExchanges.isSelected = filter == Filter.EXCHANGES

        if (filter == this.filter && currentCache != null) {
            (currentCache as? ViewportCache<*>)?.refresh()
            return
        }

        this.filter = filter

        val setup = mapSetupController ?: return

        destroyCurrentCache()
        setup.merchantsSource.setGeoJson(EMPTY_GEOJSON)
        setup.eventsSource.setGeoJson(EMPTY_GEOJSON)
        setup.exchangesSource.setGeoJson(EMPTY_GEOJSON)

        binding.map.getMapAsync { map ->
            when (filter) {
                Filter.MERCHANTS -> showCache(map, setup.merchantsSource) { MerchantsCache(map, db()) }
                Filter.EVENTS -> showCache(map, setup.eventsSource) { EventsCache(map, db()) }
                Filter.EXCHANGES -> showCache(map, setup.exchangesSource) { ExchangesCache(map, db()) }
            }
        }
    }

    private fun usingOpenFreeMap(): Boolean {
        val nightMode =
            requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        return when (prefs.mapStyle) {
            MapStyle.Auto -> !nightMode
            MapStyle.CartoDarkMatter -> false
            else -> true
        }
    }

    private fun showCache(map: MapLibreMap, source: GeoJsonSource, factory: () -> ViewportCache<*>) {
        val cache = factory()
        currentCache = cache
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                cache.geoJson.collectLatest { geoJson ->
                    source.setGeoJson(geoJson)
                }
            }
        }
    }

    private fun destroyCurrentCache() {
        (currentCache as? MerchantsCache)?.destroy()
        (currentCache as? ExchangesCache)?.destroy()
        (currentCache as? EventsCache)?.destroy()
        currentCache = null
    }

    private lateinit var areasAdapter: AreasAdapter

    private suspend fun loadAreas(lat: Double, lon: Double) {
        try {
            val areas = withContext(Dispatchers.IO) {
                api().getAreas(lat, lon).filter { it.type == "community" || it.type == "country" }
            }
            areasAdapter.submitList(areas)
        } catch (_: Throwable) {
            areasAdapter.submitList(emptyList())
        }
    }

    private fun initInsets(binding: MapFragmentBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabContainer) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                rightMargin = insets.right + dpToPx(24)
                bottomMargin = insets.bottom + dpToPx(24)
                leftMargin = insets.left
            }

            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.buttonGroup) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                rightMargin = insets.right
                bottomMargin = insets.bottom + dpToPx(20)
                leftMargin = insets.left + dpToPx(24)
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun navigateToSettings() {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<SettingsFragment>(R.id.fragmentContainerView)
            addToBackStack(null)
        }
    }
}
