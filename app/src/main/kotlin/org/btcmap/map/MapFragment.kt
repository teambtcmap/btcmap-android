package org.btcmap.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.LinearLayoutManager
import org.btcmap.bundle.BundledPlaces
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.btcmap.db.table.place.Place
import org.btcmap.place.PlaceFragment
import org.btcmap.activity.ActivityFeedFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.MapFragmentBinding
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.btcmap.area.AreaFragment
import org.btcmap.place.isMerchant
import org.btcmap.search.SearchAdapter
import org.btcmap.settings.MapStyle
import org.btcmap.settings.SettingsFragment
import org.btcmap.settings.mapStyle
import org.btcmap.settings.mapViewport
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.prefs
import org.btcmap.settings.showDebugInfo
import org.btcmap.settings.uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.btcmap.db
import org.btcmap.sync
import org.btcmap.db.table.place.Marker
import org.btcmap.db.table.event.Event
import org.btcmap.map.layer.createEventLayers
import org.btcmap.map.layer.createExchangeLayers
import org.btcmap.map.layer.createMerchantLayers
import org.btcmap.search.SearchAdapterItem
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import java.text.NumberFormat

class MapFragment : Fragment() {

    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private val placeFragment by lazy {
        childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private val insetsController: WindowInsetsControllerCompat? by lazy {
        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView
        )
    }

    private var backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    private var initialLoadInProgress = false
    private var dbCallCount = 0
    private var lastDbCallTimeMs = 0L
    private var lastMerchantsGeoJson: String? = null
    private var lastEventsGeoJson: String? = null
    private var lastExchangesGeoJson: String? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (it.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            onLocationPermissionsGranted(true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionsGranted(firstTime: Boolean) {
        binding.map.getMapAsync { map ->
            map.getStyle { style ->
                val locationComponentOptions =
                    LocationComponentOptions.builder(requireContext()).pulseEnabled(true).build()
                val locationComponentActivationOptions =
                    buildLocationComponentActivationOptions(style, locationComponentOptions)
                map.locationComponent.activateLocationComponent(
                    locationComponentActivationOptions
                )
                map.locationComponent.isLocationComponentEnabled = true

                if (firstTime) {
                    val lastKnownLocation = map.locationComponent.lastKnownLocation

                    if (lastKnownLocation != null) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation),
                                14.0,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildLocationComponentActivationOptions(
        style: Style, locationComponentOptions: LocationComponentOptions
    ): LocationComponentActivationOptions {
        return LocationComponentActivationOptions.builder(requireContext(), style)
            .locationComponentOptions(locationComponentOptions).useDefaultLocationEngine(true)
            .locationEngineRequest(
                LocationEngineRequest.Builder(750).setFastestInterval(750)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY).build()
            ).build()
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

    private fun initSearchBar(binding: MapFragmentBinding) {
        binding.searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_place -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = "https://btcmap.org/add-location".toUri()
                    startActivity(intent)
                }

                R.id.settings -> {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<SettingsFragment>(R.id.fragmentContainerView)
                        addToBackStack(null)
                    }
                }
            }

            true
        }
    }

    private suspend fun selectPlace(place: Place?) {
        if (place != null) {
            getPlaceDetailsToolbar()
            placeFragment.setPlace(place)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val merchantLayers = createMerchantLayers(
            markerBackgroundColor = prefs.markerBackgroundColor(requireContext()),
            markerBadgeBackgroundColor = prefs.badgeBackgroundColor(requireContext()),
            markerBadgeTextColor = prefs.badgeTextColor(requireContext()),
            usingOpenFreeMap = usingOpenFreeMap(),
        )
        val eventLayers = createEventLayers(
            markerBackgroundColor = prefs.markerBackgroundColor(requireContext()),
            usingOpenFreeMap = usingOpenFreeMap(),
        )
        val exchangeLayers = createExchangeLayers(
            markerBackgroundColor = prefs.markerBackgroundColor(requireContext()),
            markerBadgeBackgroundColor = prefs.badgeBackgroundColor(requireContext()),
            markerBadgeTextColor = prefs.badgeTextColor(requireContext()),
            usingOpenFreeMap = usingOpenFreeMap(),
        )
        merchantsSource = merchantLayers.first
        eventsSource = eventLayers.first
        exchangesSource = exchangeLayers.first
        binding.map.getMapAsync { map ->
            map.getStyle { style ->
                style.addSource(merchantLayers.first)
                merchantLayers.second.forEach { style.addLayer(it) }
                style.addSource(eventLayers.first)
                eventLayers.second.forEach { style.addLayer(it) }
                style.addSource(exchangeLayers.first)
                exchangeLayers.second.forEach { style.addLayer(it) }
            }
        }

        initSearchBar(binding)

        launchUpdateChecker()

        binding.showMerchants.setOnClickListener { setFilter(Filter.MERCHANTS) }
        binding.showEvents.setOnClickListener { setFilter(Filter.EVENTS) }
        binding.showExchanges.setOnClickListener { setFilter(Filter.EXCHANGES) }

        binding.map.getMapAsync {
            it.addOnCameraIdleListener {
                if (initialLoadInProgress) {
                    initialLoadInProgress = false
                }
                prefs.mapViewport = it.projection.visibleRegion.latLngBounds
                val center = it.projection.visibleRegion.latLngBounds.center
                viewLifecycleOwner.lifecycleScope.launch {
                    loadAreas(center.latitude, center.longitude)
                }
                when (filter) {
                    Filter.MERCHANTS -> showMerchants()
                    Filter.EVENTS -> showEvents()
                    Filter.EXCHANGES -> showExchanges()
                }
            }

            it.setStyle(Style.Builder().fromUri(prefs.mapStyle.uri(requireContext())))
            it.uiSettings.setCompassMargins(dpToPx(0), dpToPx(120 + 16), dpToPx(16), dpToPx(0))
            it.uiSettings.isLogoEnabled = false
            it.uiSettings.isAttributionEnabled = false
            it.uiSettings.isTiltGesturesEnabled = false
            it.addMarkerClickListener()

            it.getStyle { style ->
                if (style.getImage("btcmap-marker") == null) {
                    val markerDrawable =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.map_marker)!!
                    DrawableCompat.setTint(
                        markerDrawable,
                        prefs.markerBackgroundColor(requireContext())
                    )

                    style.addImage(
                        "btcmap-marker",
                        markerDrawable,
                    )
                }

                if (style.getImage("btcmap-marker-boosted") == null) {
                    val markerDrawable =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.map_marker)!!
                    DrawableCompat.setTint(
                        markerDrawable,
                        prefs.boostedMarkerBackgroundColor()
                    )

                    style.addImage(
                        "btcmap-marker-boosted",
                        markerDrawable,
                    )
                }

                init(requireContext(), style)
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.elementDetails)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addSlideCallback()

        viewLifecycleOwner.lifecycleScope.launch {
            val elementDetailsToolbar = getPlaceDetailsToolbar() ?: return@launch
            bottomSheetBehavior.peekHeight = elementDetailsToolbar.height * 3
            bottomSheetBehavior.halfExpandedRatio = 0.33f
            bottomSheetBehavior.isFitToContents = false
            bottomSheetBehavior.skipCollapsed = true

            elementDetailsToolbar.setOnClickListener {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onLocationPermissionsGranted(false)
        }

        binding.fab.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermissions()
                return@setOnClickListener
            }

            binding.map.getMapAsync { map ->
                val lastKnownLocation = map.locationComponent.lastKnownLocation

                if (lastKnownLocation != null) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(lastKnownLocation),
                            14.0,
                        )
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.sync.isVisible = true

                val importResult = BundledPlaces.import(requireContext(), db())
                Log.d("import", importResult.toString())

                if (importResult.placesImported > 0) {
                    setFilter(filter)
                }

                if (sync().syncPlaces().rowsAffected > 0) {
                    setFilter(filter)
                }

                if (sync().syncEvents().rowsAffected > 0) {
                    setFilter(filter)
                }

                if (sync().syncComments().rowsAffected > 0) {
                    setFilter(filter)
                }

                binding.sync.isVisible = false
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, backPressedCallback
        )

        initialLoadInProgress = true

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

        searchResults.onEach {
            searchAdapter.submitList(it) {
                val layoutManager = binding.searchResults.layoutManager ?: return@submitList
                layoutManager.scrollToPosition(0)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        areasAdapter = AreasAdapter { area ->
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<AreaFragment>(
                    R.id.fragmentContainerView,
                    null,
                    bundleOf("area_id" to area.id.toString())
                )
                addToBackStack(null)
            }
        }

        binding.areas.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                true,
            )
        binding.areas.adapter = areasAdapter

        binding.activityFeed.setOnClickListener {
            val areaIds = areasAdapter.currentList.map { it.urlAlias }
            if (areaIds.isNotEmpty()) {
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<ActivityFeedFragment>(
                        R.id.fragmentContainerView,
                        null,
                        bundleOf("area_ids" to ArrayList(areaIds))
                    )
                    addToBackStack(null)
                }
            }
        }

        binding.searchView.editText.doAfterTextChanged { searchString ->
            binding.map.getMapAsync { map ->
                viewLifecycleOwner.lifecycleScope.launch {
                    search(
                        referenceLocation = map.projection.visibleRegion.latLngBounds.center,
                        searchString = searchString.toString(),
                    )
                }
            }
        }

//        viewLifecycleOwner.lifecycleScope.launch {
//            val merchants =
//                withContext(Dispatchers.IO) { PlaceQueries.selectMerchants(db).toGeoJson() }
//            merchantsSource.setGeoJson(merchants)
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

    private fun onBackPressed() {
        when (bottomSheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }

            else -> {
                requireActivity().finish()
            }
        }
    }

    private fun MapLibreMap.addMarkerClickListener() {
        val layerIds = listOf(
            LAYER_UNCLUSTERED_MERCHANTS,
            LAYER_MERCHANTS_CATEGORY_ICONS,
            LAYER_EXCHANGES,
            LAYER_EXCHANGES_CATEGORY_ICONS,
            LAYER_EVENTS,
            LAYER_EVENTS_CATEGORY_ICONS
        )

        addOnMapClickListener { point ->
            val screenLocation = projection.toScreenLocation(point)
            val features = queryRenderedFeatures(screenLocation, *layerIds.toTypedArray())

            if (features.isEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch { selectPlace(null) }
                return@addOnMapClickListener false
            }

            val feature = features[0]

            try {
                val isEvent =
                    feature.getProperty("iconId") == null && feature.getProperty("count") == null

                if (isEvent) {
                    val idValue = feature.getProperty("id")
                    if (idValue != null) {
                        val eventId = idValue.asLong
                        val event = db().event.selectById(eventId)
                        if (event != null) {
                            val intent =
                                Intent(Intent.ACTION_VIEW, event.website.toString().toUri())
                            startActivity(intent)
                            return@addOnMapClickListener true
                        }
                    }
                }

                val idValue = feature.getProperty("id")

                if (idValue != null) {
                    val placeId = idValue.asLong
                    val place = db().place.selectById(placeId)
                    viewLifecycleOwner.lifecycleScope.launch { selectPlace(place) }
                    return@addOnMapClickListener true
                }
            } catch (e: Exception) {
                Log.e(null, null, e)
            }

            false
        }
    }

    private fun BottomSheetBehavior<*>.addSlideCallback() {
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    styleStatusBar()
                }

                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    viewLifecycleOwner.lifecycleScope.launch { selectPlace(null) }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                placeFragment.onSlide(slideOffset)
                styleStatusBar()
            }
        })
    }

    private suspend fun getPlaceDetailsToolbar(): Toolbar? {
        var attempts = 0

        while (placeFragment.view == null || placeFragment.requireView()
                .findViewById<View>(R.id.toolbar)!!.height == 0
        ) {
            if (attempts >= 100) {
                requireActivity().finish()
                return null
            } else {
                attempts++
                delay(10)
            }
        }

        return placeFragment.requireView().findViewById(R.id.toolbar)!!
    }

    override fun onResume() {
        super.onResume()
        styleStatusBar()
    }

    override fun onPause() {
        super.onPause()
        restoreDefaultStatusBar()
    }

    fun styleStatusBar() {
        val nightMode =
            requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            insetsController?.isAppearanceLightStatusBars = !nightMode
        } else {
            when (prefs.mapStyle) {
                MapStyle.Auto -> {
                    insetsController?.isAppearanceLightStatusBars = !nightMode
                }

                MapStyle.Dark,
                MapStyle.CartoDarkMatter -> insetsController?.isAppearanceLightStatusBars = false

                else -> insetsController?.isAppearanceLightStatusBars = true
            }
        }
    }

    fun restoreDefaultStatusBar() {
        val nightMode =
            requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        insetsController?.isAppearanceLightStatusBars = !nightMode
    }

    private fun List<Marker>.toGeoJson(): String {
        val sb = StringBuilder()
        sb.append(
            """
        {
            "type": "FeatureCollection",
            "features": [
        """.trimIndent()
        )

        this.forEachIndexed { index, place ->
            if (index > 0) {
                sb.append(",")
            }
            sb.append(
                """
            {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [${place.lon}, ${place.lat}]
                },
                "properties": {
                    "id": ${place.id},
                    "count": 1,
                    "iconId": "${place.icon}",
                    "requiresCompanionApp": ${place.requiredAppUrl != null},
                    "comments": ${place.comments},
                    "boosted": ${place.boostedUntil != null}
                }
            }
        """.trimIndent()
            )
        }

        sb.append(
            """
            ]
        }
        """.trimIndent()
        )

        return sb.toString()
    }

    private fun List<Event>.toEventsGeoJson(): String {
        val sb = StringBuilder()
        sb.append(
            """
        {
            "type": "FeatureCollection",
            "features": [
        """.trimIndent()
        )

        this.forEachIndexed { index, event ->
            if (index > 0) {
                sb.append(",")
            }
            sb.append(
                """
            {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [${event.lon}, ${event.lat}]
                },
                "properties": {
                    "id": ${event.id}
                }
            }
        """.trimIndent()
            )
        }

        sb.append(
            """
            ]
        }
        """.trimIndent()
        )

        return sb.toString()
    }

    private enum class Filter {
        MERCHANTS,
        EVENTS,
        EXCHANGES,
    }

    private var filter = Filter.MERCHANTS

    private fun setFilter(filter: Filter) {
        Log.d("MapFragment", "setFilter called with: $filter")
        this.filter = filter

        binding.showMerchants.isSelected = filter == Filter.MERCHANTS
        binding.showEvents.isSelected = filter == Filter.EVENTS
        binding.showExchanges.isSelected = filter == Filter.EXCHANGES

        merchantsCache = PlaceCache()
        exchangesCache = PlaceCache()
        eventsCache = EventCache()
        dbCallCount = 0
        lastDbCallTimeMs = 0L

        if (initialLoadInProgress) {
            Log.d("MapFragment", "setFilter: skipping show due to initial load")
            return
        }

        when (filter) {
            Filter.MERCHANTS -> showMerchants()
            Filter.EVENTS -> showEvents()
            Filter.EXCHANGES -> showExchanges()
        }
    }

    private lateinit var merchantsSource: GeoJsonSource
    private lateinit var eventsSource: GeoJsonSource
    private lateinit var exchangesSource: GeoJsonSource

    private fun usingOpenFreeMap(): Boolean {
        val nightMode =
            requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        return when (prefs.mapStyle) {
            MapStyle.Auto -> !nightMode
            MapStyle.CartoDarkMatter -> false
            else -> true
        }
    }

    private fun showMerchants() {
        clearOtherSources(merchantsSource)
        binding.map.getMapAsync { map ->
            val bounds = map.projection.visibleRegion.latLngBounds
            val expandedBounds = expandBounds(bounds, CLUSTERING_SCALE_FACTOR)
            Log.d(
                "MapFragment",
                "showMerchants: bounds=$bounds, expanded=$expandedBounds, cacheBounds=${merchantsCache.bounds}"
            )
            viewLifecycleOwner.lifecycleScope.launch {
                if (!merchantsCache.contains(expandedBounds)) {
                    Log.d("MapFragment", "showMerchants: cache miss, fetching")
                    dbCallCount++
                    val startTime = System.currentTimeMillis()
                    val newMerchants = withContext(Dispatchers.IO) {
                        db().place.selectMerchantsByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            expandedBounds.longitudeWest,
                            expandedBounds.longitudeEast,
                        )
                    }
                    lastDbCallTimeMs = System.currentTimeMillis() - startTime
                    Log.d(
                        "MapFragment",
                        "showMerchants: fetched ${newMerchants.size} merchants in ${lastDbCallTimeMs}ms"
                    )
                    merchantsCache = merchantsCache.add(newMerchants, expandedBounds)
                } else {
                    Log.d("MapFragment", "showMerchants: cache hit")
                }
                val geoJson = merchantsCache.features.toGeoJson()
                if (geoJson != lastMerchantsGeoJson) {
                    lastMerchantsGeoJson = geoJson
                    merchantsSource.setGeoJson(geoJson)
                }
                updateDebugStats()
            }
        }
    }

    private fun showEvents() {
        clearOtherSources(eventsSource)
        binding.map.getMapAsync { map ->
            val bounds = map.projection.visibleRegion.latLngBounds
            val expandedBounds = expandBounds(bounds, CLUSTERING_SCALE_FACTOR)
            viewLifecycleOwner.lifecycleScope.launch {
                if (!eventsCache.contains(expandedBounds)) {
                    dbCallCount++
                    val startTime = System.currentTimeMillis()
                    val newEvents = withContext(Dispatchers.IO) {
                        db().event.selectByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            expandedBounds.longitudeWest,
                            expandedBounds.longitudeEast,
                        )
                    }
                    lastDbCallTimeMs = System.currentTimeMillis() - startTime
                    eventsCache = eventsCache.add(newEvents, expandedBounds)
                }
                val geoJson = eventsCache.features.toEventsGeoJson()
                if (geoJson != lastEventsGeoJson) {
                    lastEventsGeoJson = geoJson
                    eventsSource.setGeoJson(geoJson)
                }
                updateDebugStats()
            }
        }
    }

    private fun showExchanges() {
        clearOtherSources(exchangesSource)
        binding.map.getMapAsync { map ->
            val bounds = map.projection.visibleRegion.latLngBounds
            val expandedBounds = expandBounds(bounds, CLUSTERING_SCALE_FACTOR)
            viewLifecycleOwner.lifecycleScope.launch {
                if (!exchangesCache.contains(expandedBounds)) {
                    dbCallCount++
                    val startTime = System.currentTimeMillis()
                    val newExchanges = withContext(Dispatchers.IO) {
                        db().place.selectExchangesByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            expandedBounds.longitudeWest,
                            expandedBounds.longitudeEast,
                        )
                    }
                    lastDbCallTimeMs = System.currentTimeMillis() - startTime
                    exchangesCache = exchangesCache.add(newExchanges, expandedBounds)
                }
                val geoJson = exchangesCache.features.toGeoJson()
                if (geoJson != lastExchangesGeoJson) {
                    lastExchangesGeoJson = geoJson
                    exchangesSource.setGeoJson(geoJson)
                }
                updateDebugStats()
            }
        }
    }

    private fun expandBounds(bounds: LatLngBounds, scaleFactor: Double): LatLngBounds {
        val latSpan = bounds.latitudeSpan * scaleFactor
        val lonSpan = bounds.longitudeSpan * scaleFactor
        val center = bounds.center
        val latNorth = (center.latitude + latSpan / 2).coerceAtMost(90.0)
        val latSouth = (center.latitude - latSpan / 2).coerceAtLeast(-90.0)
        return LatLngBounds.from(
            latNorth = latNorth,
            lonEast = center.longitude + lonSpan / 2,
            latSouth = latSouth,
            lonWest = center.longitude - lonSpan / 2,
        )
    }

    private fun updateDebugStats() {
        val cacheSize = when (filter) {
            Filter.MERCHANTS -> merchantsCache.features.size
            Filter.EVENTS -> eventsCache.features.size
            Filter.EXCHANGES -> exchangesCache.features.size
        }
        val currentBounds = when (filter) {
            Filter.MERCHANTS -> merchantsCache.bounds
            Filter.EVENTS -> eventsCache.bounds
            Filter.EXCHANGES -> exchangesCache.bounds
        }
        val viewportInfo = if (currentBounds != null) {
            val latSpanKm = currentBounds.latitudeSpan * 111
            val lonSpanKm =
                currentBounds.longitudeSpan * 111 * kotlin.math.cos(Math.toRadians(currentBounds.center.latitude))
            "%.1fkm x %.1fkm".format(latSpanKm, lonSpanKm)
        } else {
            "no bounds"
        }
        Log.d("MapFragment", "updateDebugStats: filter=$filter, cacheSize=$cacheSize")
        if (prefs.showDebugInfo) {
            binding.debugStats.apply {
                text =
                    "memcache: %d items\nmemcache bounds: %s\ndb queries: %d\nlast query: %dms".format(
                        cacheSize, viewportInfo, dbCallCount, lastDbCallTimeMs
                    )
                isVisible = true
            }
        } else {
            binding.debugStats.isVisible = false
        }
    }

    private fun clearOtherSources(activeSource: GeoJsonSource) {
        when (activeSource) {
            merchantsSource -> {
                eventsSource.setGeoJson(EMPTY_GEOJSON)
                exchangesSource.setGeoJson(EMPTY_GEOJSON)
            }

            eventsSource -> {
                merchantsSource.setGeoJson(EMPTY_GEOJSON)
                exchangesSource.setGeoJson(EMPTY_GEOJSON)
            }

            exchangesSource -> {
                merchantsSource.setGeoJson(EMPTY_GEOJSON)
                eventsSource.setGeoJson(EMPTY_GEOJSON)
            }
        }
    }

    private fun launchUpdateChecker() {
        viewLifecycleOwner.lifecycleScope.launch {
            withResumed {
                binding.update.isVisible = false

                launch {
                    try {
                        val latestVerJson = OkHttpClient.Builder().build().newCall(
                            Request.Builder()
                                .url("https://static.btcmap.org/android/latest-app-ver".toHttpUrl())
                                .build()
                        ).executeAsync().body.string().trim()

                        val latestVer =
                            com.google.gson.JsonParser.parseString(latestVerJson).asJsonObject
                        val latestVerCode = latestVer.get("code").asInt
                        val latestVerName = latestVer.get("name").asString
                        val latestVerUrl = latestVer.get("url").asString

                        if (latestVerCode > BuildConfig.VERSION_CODE) {
                            withResumed {
                                binding.update.isVisible = true
                                binding.update.iconColor(requireContext().getErrorColor())

                                binding.update.setOnClickListener {
                                    MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.update_available)
                                        .setMessage(
                                            getString(
                                                R.string.update_available_description,
                                                BuildConfig.VERSION_NAME, latestVerName
                                            )
                                        )
                                        .setPositiveButton(R.string.get_apk) { _, _ ->
                                            val intent = Intent(Intent.ACTION_VIEW)
                                            intent.data = latestVerUrl.toUri()
                                            startActivity(intent)
                                        }
                                        .setNegativeButton(R.string.ignore, null)
                                        .show()
                                }
                            }
                        }
                    } catch (_: Throwable) {

                    }
                }
            }
        }
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 3
        private const val CLUSTERING_SCALE_FACTOR = 2.0

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }

        const val LAYER_UNCLUSTERED_MERCHANTS = "unclusteredMerchants"
        const val LAYER_MERCHANTS_CATEGORY_ICONS = "unclusteredMerchantsCategoryIcons"
        const val LAYER_EXCHANGES = "exchanges"
        const val LAYER_EXCHANGES_CATEGORY_ICONS = "exchangesCategoryIcons"
        const val LAYER_EVENTS = "events"
        const val LAYER_EVENTS_CATEGORY_ICONS = "eventsCategoryIcons"
        val EMPTY_GEOJSON = """{"type":"FeatureCollection","features":[]}"""
        const val ICON_OFFSET_Y = -29f
    }

    private data class PlaceCache(
        val features: List<Marker> = emptyList(),
        val bounds: LatLngBounds? = null,
    ) {
        fun contains(bounds: LatLngBounds): Boolean {
            if (this.bounds == null) return false
            return this.bounds.contains(bounds)
        }

        fun add(newFeatures: List<Marker>, newBounds: LatLngBounds): PlaceCache {
            val mergedBounds = if (bounds == null) {
                newBounds
            } else {
                LatLngBounds.Builder()
                    .include(LatLng(bounds.latitudeNorth, bounds.longitudeWest))
                    .include(LatLng(bounds.latitudeSouth, bounds.longitudeEast))
                    .include(LatLng(newBounds.latitudeNorth, newBounds.longitudeWest))
                    .include(LatLng(newBounds.latitudeSouth, newBounds.longitudeEast))
                    .build()
            }
            val existingIds = features.map { it.id }.toSet()
            val uniqueNewFeatures = newFeatures.filter { it.id !in existingIds }
            return PlaceCache(features + uniqueNewFeatures, mergedBounds)
        }
    }

    private data class EventCache(
        val features: List<Event> = emptyList(),
        val bounds: LatLngBounds? = null,
    ) {
        fun contains(bounds: LatLngBounds): Boolean {
            if (this.bounds == null) return false
            return this.bounds.contains(bounds)
        }

        fun add(newFeatures: List<Event>, newBounds: LatLngBounds): EventCache {
            val mergedBounds = if (bounds == null) {
                newBounds
            } else {
                LatLngBounds.Builder()
                    .include(LatLng(bounds.latitudeNorth, bounds.longitudeWest))
                    .include(LatLng(bounds.latitudeSouth, bounds.longitudeEast))
                    .include(LatLng(newBounds.latitudeNorth, newBounds.longitudeWest))
                    .include(LatLng(newBounds.latitudeSouth, newBounds.longitudeEast))
                    .build()
            }
            val existingIds = features.map { it.id }.toSet()
            val uniqueNewFeatures = newFeatures.filter { it.id !in existingIds }
            return EventCache(features + uniqueNewFeatures, mergedBounds)
        }
    }

    private var merchantsCache = PlaceCache()
    private var exchangesCache = PlaceCache()
    private var eventsCache = EventCache()

    private val _searchResults = MutableStateFlow<List<SearchAdapterItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private lateinit var areasAdapter: AreasAdapter

    suspend fun search(referenceLocation: LatLng, searchString: String) {
        if (searchString.length < MIN_QUERY_LENGTH) {
            _searchResults.update { emptyList() }
        } else {
            val unsortedPlaces = withContext(Dispatchers.IO) {
                db().place.selectBySearchString(searchString)
            }

            val sortedPlaces = unsortedPlaces.sortedBy {
                getDistanceInMeters(
                    startLatitude = referenceLocation.latitude,
                    startLongitude = referenceLocation.longitude,
                    endLatitude = it.lat,
                    endLongitude = it.lon,
                )
            }

            _searchResults.update { sortedPlaces.map { it.toAdapterItem(referenceLocation) } }
        }
    }

    private fun Place.toAdapterItem(referenceLocation: LatLng): SearchAdapterItem {
        val distanceMeters = referenceLocation.distanceTo(LatLng(lat, lon))

        val distanceString = if (distanceMeters < 1_000) {
            resources.getString(R.string.s_m, DISTANCE_FORMAT.format(distanceMeters))
        } else {
            resources.getString(R.string.s_km, DISTANCE_FORMAT.format(distanceMeters / 1_000))
        }

        return SearchAdapterItem(
            placeId = this.id,
            icon = this.icon,
            name = this.name ?: "",
            distanceToUser = distanceString,
        )
    }

    private fun getDistanceInMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Double {
        val distance = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance)
        return distance[0].toDouble()
    }

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

    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}