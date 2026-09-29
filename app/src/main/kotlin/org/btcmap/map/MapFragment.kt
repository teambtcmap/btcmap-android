package org.btcmap.map

import android.Manifest
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.SyncEvent
import org.btcmap.SyncState
import org.btcmap.feed.ActivityFeedFragment
import org.btcmap.api
import org.btcmap.api.getEvent
import org.btcmap.api.getPlaceCoordinates
import org.btcmap.area.ARG_AREA_ID
import org.btcmap.area.AreaFragment
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.bundle.BundledBasemap
import org.btcmap.db
import org.btcmap.db.table.place.Place
import org.btcmap.databinding.MapFragmentBinding
import org.btcmap.event.EventFragment
import org.btcmap.event.toBundle
import org.btcmap.place.AddPlaceFragment
import org.btcmap.place.PlaceFragment
import org.btcmap.place.isMerchant
import org.btcmap.search.SearchAdapter
import org.btcmap.search.SearchAdapterItem
import org.btcmap.settings.SettingsFragment
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.mapStyle
import org.btcmap.settings.mapViewport
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.prefs
import org.btcmap.settings.showAttribution
import org.btcmap.settings.uri
import org.btcmap.syncController
import org.btcmap.util.DeepLink
import org.btcmap.util.isOnline
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView

class MapFragment : Fragment() {
    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private var statusBarController: MapStatusBarController? = null
    private var bottomSheetController: BottomSheetController? = null
    private var updateNotificationController: UpdateNotificationController? = null

    private var currentCache: ViewportCache<*>? = null
    private var mapSelectionController: MapSelectionController? = null
    private var mapSetupController: MapSetupController? = null
    private var locationController: LocationController? = null

    private lateinit var searchController: SearchController
    private lateinit var mapAreasController: MapAreasController

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // The result can be delivered after the view is gone (the callback is
        // not view-lifecycle-scoped); there is nothing left to build a location
        // component on, and touching the binding would throw.
        if (_binding == null) return@registerForActivityResult
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
        super.onViewCreated(view, savedInstanceState)

        // Restore the filter before the map's first position settles, so a
        // rotation does not drop the list the user was looking at. On a
        // back-stack return there is no saved state (the fragment instance
        // survives, only its view is recreated), so the field keeps the filter
        // the user last chose; a fresh instance defaults to merchants.
        filter = savedInstanceState?.getString(STATE_FILTER)
            ?.let { name -> runCatching { Filter.valueOf(name) }.getOrNull() }
            ?: filter

        // Registered here, not when the dialog is shown, so a form that was open
        // when the device rotated still reaches this (recreated) fragment.
        registerAuthResultListener { navigateToAddPlace() }

        // The MapView owns native resources and must receive its lifecycle
        // callbacks; without onDestroy in particular, reopening the map leaves a
        // dead renderer behind and later queries can crash natively.
        binding.map.onCreate(savedInstanceState)

        // Capture the viewport to restore before any camera idle can overwrite
        // it, so the restore does not race the map's default camera. moveTo
        // clears it for a deep link or a search selection on this view.
        viewportToRestore = prefs.mapViewport
        mapPositioned = false

        searchController = SearchController(
            db = db(),
            resources = resources,
        )

        mapAreasController = MapAreasController(db = db())

        val bottomSheet = BottomSheetController(
            view = binding.placeBottomSheet,
            viewLifecycleOwner = viewLifecycleOwner,
            placeFragment = childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment,
        )
        bottomSheetController = bottomSheet

        statusBarController = MapStatusBarController(
            conf = resources.configuration,
            insetsController = WindowCompat.getInsetsController(
                requireActivity().window,
                requireActivity().window.decorView,
            ),
            bottomSheetBehavior = bottomSheet.bottomSheetBehavior,
        )
        statusBarController?.onViewCreated()

        updateNotificationController = UpdateNotificationController(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            icon = binding.update,
        )

        binding.searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_place -> openAddPlace()
                R.id.settings -> navigateToSettings()
            }
            true
        }

        binding.attribution.isVisible = prefs.showAttribution
        binding.attribution.setOnClickListener {
            openInBrowser(getString(R.string.osm_attribution_url).toUri())
        }

        val app = requireContext().applicationContext as App
        val styleUri = app.mapStyleUriForTesting ?: prefs.mapStyle.uri(requireContext())
        mapSetupController = MapSetupController(
            mapView = binding.map,
            styleUri = styleUri,
            bundledStyle = bundledBasemapStyle(app, styleUri),
            markerBackgroundColor = prefs.markerBackgroundColor(requireContext()),
            markerBadgeBackgroundColor = prefs.badgeBackgroundColor(requireContext()),
            markerBadgeTextColor = prefs.badgeTextColor(requireContext()),
            boostedMarkerBackgroundColor = prefs.boostedMarkerBackgroundColor(),
            usingOpenFreeMap = app.mapStyleUriForTesting == null,
            rotationEnabled = prefs.mapRotationEnabled,
        ).also {
            it.install()
            it.setOffline(!requireContext().isOnline())
        }
        registerConnectivity()

        binding.showMerchants.setOnClickListener { setFilter(Filter.MERCHANTS) }
        binding.showEvents.setOnClickListener { setFilter(Filter.EVENTS) }
        binding.showExchanges.setOnClickListener { setFilter(Filter.EXCHANGES) }

        binding.map.getMapAsync { map ->
            if (_binding == null) return@getMapAsync
            map.addOnCameraIdleListener {
                if (_binding == null) return@addOnCameraIdleListener
                val bounds = map.projection.visibleRegion.latLngBounds
                // The default camera is not a position the user chose: saving it
                // would replace the stored viewport with the world view and the
                // next open would zoom all the way out.
                if (mapPositioned) prefs.mapViewport = bounds
                mapAreasController.load(bounds.center.latitude, bounds.center.longitude)
            }

            // The setup controller owns the per-map marker image registry, so
            // hit-testing rejects taps through a marker's transparent pixels
            // using the same images the renderer was given.
            val setup = mapSetupController ?: return@getMapAsync
            mapSelectionController = MapSelectionController(
                map = map,
                db = db(),
                markerImageRegistry = setup.markerImageRegistry,
                onOpenPlace = ::selectPlace,
                onOpenEvent = { openEvent(it.toBundle()) },
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
                // The sync is app-scoped: starting it here only kicks it off, and
                // it keeps running after this screen is closed. Each resume starts
                // a fresh one unless the previous run is still going.
                syncController().start()

                launch {
                    syncController().state.collect { state ->
                        binding.sync.setVisibleAnimated(state != SyncState.Idle)
                    }
                }

                syncController().events.collect { event ->
                    when (event) {
                        SyncEvent.PlacesChanged -> rebuildCurrentCache()
                        SyncEvent.EventsChanged -> {
                            if (filter == Filter.EVENTS) rebuildCurrentCache()
                            // The area chips display each area's upcoming event
                            // count, so a changed event table makes them stale
                            // even though the areas themselves did not change.
                            mapAreasController.reload()
                        }
                        SyncEvent.CommentsChanged ->
                            if (filter == Filter.MERCHANTS || filter == Filter.EXCHANGES) {
                                rebuildCurrentCache()
                            }
                        SyncEvent.AreasChanged -> mapAreasController.reload()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                binding.map.getMapAsync { map ->
                    if (_binding == null) return@getMapAsync
                    viewportToRestore?.let { bounds ->
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
                        // Restoring is the first intentional position, so camera
                        // idles are persisted from here on. When moveTo cleared
                        // the viewport, it sets this once its own move runs.
                        mapPositioned = true
                    }
                    setFilter(filter)
                }
            }
        }

        val searchAdapter = SearchAdapter { row ->
            binding.searchView.clearText()
            binding.searchView.hide()

            when (row) {
                is SearchAdapterItem.Place -> openPlace(row)
                is SearchAdapterItem.Area -> openArea(row)
                is SearchAdapterItem.Event -> openEventById(row.eventId)
            }
        }

        binding.searchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResults.adapter = searchAdapter

        searchController.results.onEach {
            searchAdapter.submitList(it) {
                // The diff commits after the collector may have been cancelled
                // by view destruction, so the binding can be gone by now.
                val layoutManager = _binding?.searchResults?.layoutManager ?: return@submitList
                layoutManager.scrollToPosition(0)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        areasAdapter = AreasAdapter(apiUrl = prefs.apiUrl) { area ->
            openArea(area.id)
        }

        // Top-down, so the first item in the adapter (a country before its
        // communities) is the top chip in the bottom-anchored stack.
        binding.areas.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false,
        )
        binding.areas.adapter = areasAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapAreasController.areas.collect { areasAdapter.submitList(it) }
            }
        }

        binding.activityFeed.setOnClickListener {
            val areaIds = areasAdapter.currentList.map { it.urlAlias }
            val areaNames = areasAdapter.currentList.map { it.name }
            val areaTypes = areasAdapter.currentList.map { it.type }
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<ActivityFeedFragment>(
                    R.id.fragmentContainerView, null, Bundle().apply {
                        putStringArrayList("area_ids", ArrayList(areaIds))
                        putStringArrayList("area_names", ArrayList(areaNames))
                        putStringArrayList("area_types", ArrayList(areaTypes))
                    }
                )
                addToBackStack(null)
            }
        }

        binding.searchView.editText.doAfterTextChanged { searchString ->
            val text = searchString.toString()
            searchDebounceJob?.cancel()
            searchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                binding.map.getMapAsync { map ->
                    searchController.search(
                        referenceLocation = map.projection.visibleRegion.latLngBounds.center,
                        query = text,
                    )
                }
            }
        }

        val deepLink = (activity as? Activity)?.consumeDeepLink()
        when (deepLink) {
            is DeepLink.Place -> openPlaceById(deepLink.id)
            is DeepLink.Event -> openEventById(deepLink.id)
            null -> {}
        }

        // After the deep link, so a fresh link still wins: the restore only runs
        // on a recreation, where the Activity does not re-deliver the link.
        restoreBottomSheet(savedInstanceState)
    }

    /**
     * Re-selects the place the sheet was showing before the view was recreated,
     * and restores its position, so a rotation or a return from another screen
     * does not dismiss it. A hidden sheet is left closed: the user dismissed it,
     * or nothing was selected.
     */
    private fun restoreBottomSheet(savedInstanceState: Bundle?) {
        // A rotation delivers the values through the saved state. A back-stack
        // return has none (the fragment instance survives, only its view is
        // recreated), so they come from the retained fields instead.
        val placeId = savedInstanceState?.getLong(STATE_PLACE_ID, 0L) ?: selectedPlaceId ?: 0L
        val sheetState =
            savedInstanceState?.getInt(STATE_SHEET_STATE, BottomSheetBehavior.STATE_HIDDEN)
                ?: lastSheetState
        if (placeId <= 0L || sheetState == BottomSheetBehavior.STATE_HIDDEN) return

        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) { db().place.selectById(placeId) }
                ?: return@launch

            selectPlace(place)
            if (sheetState == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetController?.bottomSheetBehavior?.state =
                    BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun selectPlace(place: Place) {
        selectedPlaceId = place.id
        val placeFragment =
            childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment
        placeFragment.setPlace(place)
        bottomSheetController?.halfExpand()
    }

    private fun openEvent(bundle: Bundle) {
        parentFragmentManager.executePendingTransactions()

        val current = parentFragmentManager.findFragmentById(R.id.fragmentContainerView)
        if (current is EventFragment && current.eventId == bundle.getLong("id")) return

        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<EventFragment>(R.id.fragmentContainerView, null, bundle)
            addToBackStack(null)
        }
    }

    fun openEventById(eventId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bundle = withContext(Dispatchers.IO) {
                    val local = db().event.selectById(eventId)?.toBundle()
                    if (local != null) {
                        local
                    } else {
                        try {
                            api().getEvent(eventId).toBundle()
                        } catch (t: Throwable) {
                            t.rethrowIfCancellation()
                            null
                        }
                    }
                } ?: return@launch

                openEvent(bundle)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
            }
        }
    }

    private fun openPlace(row: SearchAdapterItem.Place) {
        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) {
                db().place.selectById(row.placeId)
            } ?: return@launch

            if (place.isMerchant()) {
                binding.showMerchants.performClick()
            } else {
                binding.showExchanges.performClick()
            }

            selectPlace(place)
            moveTo(place.lat, place.lon)
        }
    }

    fun openPlaceById(placeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) { db().place.selectById(placeId) }

            if (place != null) {
                if (place.isMerchant()) {
                    binding.showMerchants.performClick()
                } else {
                    binding.showExchanges.performClick()
                }

                selectPlace(place)
                moveTo(place.lat, place.lon)
                return@launch
            }

            val coordinates = try {
                withContext(Dispatchers.IO) { api().getPlaceCoordinates(placeId) }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                null
            } ?: return@launch

            moveTo(coordinates.lat, coordinates.lon)
        }
    }

    private fun moveTo(lat: Double, lon: Double) {
        // Supersede the pending restore for this view: the deep link or search
        // selection is the intended position, and the stored viewport is
        // updated from it once the camera settles.
        viewportToRestore = null
        binding.map.getMapAsync {
            it.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(lat, lon),
                    16.0,
                )
            )
            mapPositioned = true
        }
    }

    private fun openArea(row: SearchAdapterItem.Area) {
        val bbox = row.bbox
        if (bbox != null && bbox.size == 4) {
            binding.map.getMapAsync { map ->
                val bounds = LatLngBounds.from(
                    latNorth = bbox[3],
                    lonEast = bbox[2],
                    latSouth = bbox[1],
                    lonWest = bbox[0],
                )
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
            }
        } else {
            openArea(row.areaId)
        }
    }

    private fun openArea(areaId: Long) {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<AreaFragment>(
                R.id.fragmentContainerView, null,
                Bundle().apply { putLong(ARG_AREA_ID, areaId) },
            )
            addToBackStack(null)
        }
    }

    override fun onStart() {
        super.onStart()
        _binding?.map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        _binding?.map?.onResume()
    }

    override fun onPause() {
        _binding?.map?.onPause()
        super.onPause()
    }

    override fun onStop() {
        _binding?.map?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.map?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FILTER, filter.name)
        selectedPlaceId?.let { outState.putLong(STATE_PLACE_ID, it) }
        outState.putInt(
            STATE_SHEET_STATE,
            bottomSheetController?.bottomSheetBehavior?.state
                ?: BottomSheetBehavior.STATE_HIDDEN,
        )
        _binding?.map?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectivityCallback?.let { callback ->
            requireContext().getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(callback)
        }
        connectivityCallback = null
        connectivityHandler.removeCallbacks(retryChipImages)
        searchDebounceJob?.cancel()
        searchDebounceJob = null
        searchController.dispose()
        mapAreasController.dispose()
        mapSelectionController?.detach()
        mapSelectionController = null
        mapSetupController = null
        locationController?.destroy()
        locationController = null
        destroyCurrentCache()
        // Remembered so a back-stack return can restore the sheet, which has no
        // saved instance state to read from.
        lastSheetState = bottomSheetController?.bottomSheetBehavior?.state
            ?: BottomSheetBehavior.STATE_HIDDEN
        bottomSheetController = null
        statusBarController?.onDestroyView()
        statusBarController = null
        updateNotificationController = null
        _binding?.map?.onDestroy()
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
    private var searchDebounceJob: Job? = null

    /**
     * The place the bottom sheet is showing, or null when nothing is selected.
     * Saved with the view so a rotation keeps the sheet's place and position
     * instead of dismissing it.
     */
    private var selectedPlaceId: Long? = null

    /**
     * The sheet's state when the view was last destroyed. A back-stack return
     * keeps this fragment instance but has no saved instance state, so this is
     * what tells [restoreBottomSheet] whether the sheet was visible (and how).
     * A rotation reads the state from the saved bundle instead.
     */
    private var lastSheetState = BottomSheetBehavior.STATE_HIDDEN

    /**
     * The viewport to apply once the map is ready, or null once [moveTo] has
     * superseded it for the current view. It is captured when the view is
     * created so a camera idle fired by the map's default camera cannot
     * overwrite the stored viewport before the restore runs.
     */
    private var viewportToRestore: LatLngBounds? = null

    /**
     * Whether the map has been positioned on purpose (restored, or moved by a
     * deep link or a search selection). Camera idles are only persisted once
     * this is true, so the default camera is never saved as the viewport.
     */
    private var mapPositioned = false

    private fun setFilter(filter: Filter) {
        binding.showMerchants.isSelected = filter == Filter.MERCHANTS
        binding.showEvents.isSelected = filter == Filter.EVENTS
        binding.showExchanges.isSelected = filter == Filter.EXCHANGES

        if (filter == this.filter && currentCache != null) {
            currentCache?.refresh()
            return
        }

        this.filter = filter

        val setup = mapSetupController ?: return

        destroyCurrentCache()
        setup.merchantsSource.setGeoJson(EMPTY_GEOJSON)
        setup.eventsSource.setGeoJson(EMPTY_GEOJSON)
        setup.exchangesSource.setGeoJson(EMPTY_GEOJSON)

        val selected = filter
        binding.map.getMapAsync { map ->
            if (_binding == null) return@getMapAsync
            // A newer switch may have run while this callback waited for the
            // map; the last one wins, so drop the stale request.
            if (selected != filter) return@getMapAsync
            when (filter) {
                Filter.MERCHANTS -> showCache {
                    MerchantsCache(map, db(), setup.merchantsSource, ::reportMapContentDrawn) {
                        mapSetupController?.ensureMerchantMarkers(it)
                    }
                }

                Filter.EVENTS -> showCache {
                    EventsCache(map, db(), setup.eventsSource, ::reportMapContentDrawn)
                }

                Filter.EXCHANGES -> showCache {
                    ExchangesCache(map, db(), setup.exchangesSource, ::reportMapContentDrawn) {
                        mapSetupController?.ensureExchangeMarkers(it)
                    }
                }
            }
        }
    }

    /**
     * The bundled-basemap rewrite of [styleUri], or null when the archive is
     * unavailable or a test pinned a style of its own.
     */
    private fun bundledBasemapStyle(app: App, styleUri: String): BundledBasemapStyle? {
        if (app.mapStyleUriForTesting != null) return null
        val archive = app.bundledBasemapFile ?: return null
        return bundledBasemapStyleJson(
            context = requireContext(),
            styleUri = styleUri,
            pmtilesUrl = BundledBasemap.pmtilesUrl(archive),
        )
    }

    private val connectivityHandler = Handler(Looper.getMainLooper())

    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null

    // Tracks the last observed connectivity so a chip's failed image request is
    // retried only when the network comes back, not on every capability change.
    private var online = false

    /**
     * Re-runs the chips' image requests after the network comes back. Posted
     * after a short delay so it runs once the callbacks that announce the
     * network have settled and the connection is actually usable.
     */
    private val retryChipImages = Runnable {
        if (_binding != null && ::areasAdapter.isInitialized) {
            areasAdapter.refreshImages()
        }
    }

    /**
     * Flips the bundled basemap between the split (online) and all-overzoomed
     * (offline) behaviour as the network comes and goes. The map redraws the
     * hosted tiles by itself once they can be fetched again.
     */
    private fun registerConnectivity() {
        val manager = requireContext().getSystemService(ConnectivityManager::class.java) ?: return
        // Application context, not the fragment's: a callback already in flight
        // when the view is destroyed must not touch a detached fragment.
        val context = requireContext().applicationContext

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) = refresh()

            private fun refresh() {
                connectivityHandler.post {
                    val isOnline = context.isOnline()
                    // An image request made offline failed and was not retried,
                    // so a chip that fell back to its initials needs a fresh
                    // request once the network is back. The delay coalesces the
                    // burst of callbacks that announces the network and lets it
                    // become usable before the requests are re-issued.
                    if (isOnline && !online) {
                        connectivityHandler.removeCallbacks(retryChipImages)
                        connectivityHandler.postDelayed(
                            retryChipImages,
                            CHIP_IMAGE_RETRY_DELAY_MS,
                        )
                    }
                    online = isOnline
                    mapSetupController?.setOffline(!isOnline)
                }
            }
        }

        manager.registerDefaultNetworkCallback(callback)
        connectivityCallback = callback
    }

    private fun showCache(factory: () -> ViewportCache<*>) {
        val cache = factory()
        // A race between two getMapAsync callbacks can reach here twice; drop
        // whatever the previous call left behind before adopting the new one.
        destroyCurrentCache()
        currentCache = cache
    }

    private fun destroyCurrentCache() {
        // destroy() cancels the cache's own source collector with it, so no
        // collector outlives the cache or accumulates on every filter switch.
        currentCache?.destroy()
        currentCache = null
    }

    private fun rebuildCurrentCache() {
        val cache = currentCache
        if (cache == null) {
            setFilter(filter)
            return
        }
        cache.forceRebuild()
    }

    private var mapContentReported = false

    /**
     * Reports the app as fully drawn once the map has drawn its first real
     * feature snapshot. Android's default fully-drawn moment is the first
     * frame, which here is an empty map, so the default startup metric stops
     * well before the pins the user is waiting for. Deferring it to the frame
     * after the first non-empty snapshot makes the metric match the map the
     * user actually sees.
     */
    private fun reportMapContentDrawn() {
        if (mapContentReported || _binding == null) return
        mapContentReported = true

        val listener = object : MapView.OnDidFinishRenderingFrameListener {
            override fun onDidFinishRenderingFrame(
                fullyRendered: Boolean,
                frameEncodingTime: Double,
                frameRenderingTime: Double,
            ) {
                _binding?.map?.removeOnDidFinishRenderingFrameListener(this)
                (activity as? Activity)?.reportFullyDrawn()
            }
        }

        binding.map.addOnDidFinishRenderingFrameListener(listener)
    }

    private lateinit var areasAdapter: AreasAdapter

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

    private fun openAddPlace() {
        if (prefs.authorized) {
            navigateToAddPlace()
        } else {
            showAuthDialog()
        }
    }

    private fun navigateToAddPlace() {
        binding.map.getMapAsync { map ->
            val center = map.cameraPosition.target ?: return@getMapAsync
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<AddPlaceFragment>(
                    R.id.fragmentContainerView, null, Bundle().apply {
                        putDouble("lat", center.latitude)
                        putDouble("lon", center.longitude)
                    }
                )
                addToBackStack(null)
            }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        private const val STATE_FILTER = "map_filter"

        private const val STATE_PLACE_ID = "map_selected_place_id"

        private const val STATE_SHEET_STATE = "map_sheet_state"

        // Long enough for the connectivity callbacks that follow a network
        // coming back to settle before the chip images are retried.
        private const val CHIP_IMAGE_RETRY_DELAY_MS = 1_000L
    }
}
