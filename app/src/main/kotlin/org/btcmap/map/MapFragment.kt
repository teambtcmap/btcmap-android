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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.DrawableCompat
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
import org.btcmap.db
import org.btcmap.sync
import org.btcmap.db.table.place.Marker
import org.btcmap.db.table.event.Event
import org.btcmap.map.layer.EVENT_MARKER_LAYER_ID
import org.btcmap.map.layer.EXCHANGE_MARKER_LAYER_ID
import org.btcmap.map.layer.MERCHANT_MARKER_LAYER_ID
import org.btcmap.map.layer.createEventLayers
import org.btcmap.map.layer.createExchangeLayers
import org.btcmap.map.layer.createMerchantLayers
import org.btcmap.search.SearchAdapterItem
import org.btcmap.settings.apiUrl
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.verifiedFilterYears
import java.text.NumberFormat
import java.time.ZonedDateTime

class MapFragment : Fragment() {
    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    var statusBarController: MapStatusBarController? = null
    var bottomSheetController: BottomSheetController? = null
    var updateNotificationController: UpdateNotificationController? = null

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

    private fun selectPlace(place: Place) {
        val placeFragment =
            childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment
        placeFragment.setPlace(place)
        bottomSheetController?.halfExpand()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        addMapDatSourceAndLayersAsync()

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

                Log.d("map_fragment", "starting sync")
                val importResult = BundledPlaces.import(requireContext(), db())

                Log.d("map_fragment", "imported ${importResult.placesImported} places from assets")
                if (importResult.placesImported > 0) {
                    setFilter(filter)
                }

                Log.d("map_fragment", "fetching new and updated places")
                val syncPlacesRes = sync().syncPlaces()
                Log.d("map_fragment", "got ${syncPlacesRes.rowsAffected} new and updated places")
                if (syncPlacesRes.rowsAffected > 0) {
                    setFilter(filter)
                }

                if (sync().syncEvents().rowsAffected > 0 && filter == Filter.EVENTS) {
                    setFilter(filter)
                }

                val syncCommentsRes = sync().syncComments()
                Log.d(
                    "map_fragment",
                    "got ${syncCommentsRes.rowsAffected} new and updated comments"
                )
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

        searchResults.onEach {
            searchAdapter.submitList(it) {
                val layoutManager = binding.searchResults.layoutManager ?: return@submitList
                layoutManager.scrollToPosition(0)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        areasAdapter = AreasAdapter(apiUrl = prefs.apiUrl) { area ->
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
            val areaNames = areasAdapter.currentList.map { it.name }
            val areaTypes = areasAdapter.currentList.map { it.type }
            if (areaIds.isNotEmpty()) {
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<ActivityFeedFragment>(
                        R.id.fragmentContainerView,
                        null,
                        bundleOf(
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
                    search(
                        referenceLocation = map.projection.visibleRegion.latLngBounds.center,
                        searchString = searchString.toString(),
                    )
                }
            }
        }
    }

    private fun addMapDatSourceAndLayersAsync() {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

    private fun MapLibreMap.addMarkerClickListener() {
        val layerIds = listOf(
            MERCHANT_MARKER_LAYER_ID,
            EXCHANGE_MARKER_LAYER_ID,
            EVENT_MARKER_LAYER_ID,
        )

        addOnMapClickListener { point ->
            val screenLocation = projection.toScreenLocation(point)
            val features = queryRenderedFeatures(screenLocation, *layerIds.toTypedArray())

            if (features.isEmpty()) {
                bottomSheetController?.hide()
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
                    if (place != null) {
                        viewLifecycleOwner.lifecycleScope.launch { selectPlace(place) }
                    }
                    return@addOnMapClickListener true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            false
        }
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
        this.filter = filter

        binding.showMerchants.isSelected = filter == Filter.MERCHANTS
        binding.showEvents.isSelected = filter == Filter.EVENTS
        binding.showExchanges.isSelected = filter == Filter.EXCHANGES

        merchantsCache = PlaceCache()
        exchangesCache = PlaceCache()
        eventsCache = EventCache()

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
            val expandedBounds = expandBounds(bounds)
            viewLifecycleOwner.lifecycleScope.launch {
                if (!merchantsCache.contains(expandedBounds)) {
                    val newMerchants = withContext(Dispatchers.IO) {
                        db().place.selectMerchantsByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            expandedBounds.longitudeWest,
                            expandedBounds.longitudeEast,
                            minVerifiedAt = ZonedDateTime.now()
                                .minusYears(prefs.verifiedFilterYears.toLong()),
                        )
                    }
                    merchantsCache = merchantsCache.add(newMerchants, expandedBounds)
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
            val expandedBounds = expandBounds(bounds)
            viewLifecycleOwner.lifecycleScope.launch {
                if (!eventsCache.contains(expandedBounds)) {
                    val newEvents = withContext(Dispatchers.IO) {
                        db().event.selectByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            expandedBounds.longitudeWest,
                            expandedBounds.longitudeEast,
                        )
                    }
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
            val expandedBounds = expandBounds(bounds)
            viewLifecycleOwner.lifecycleScope.launch {
                if (!exchangesCache.contains(expandedBounds)) {
                    val newExchanges = withContext(Dispatchers.IO) {
                        db().place.selectExchangesByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            expandedBounds.longitudeWest,
                            expandedBounds.longitudeEast,
                        )
                    }
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

    private fun expandBounds(bounds: LatLngBounds, scaleFactor: Double = 2.0): LatLngBounds {
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

    @SuppressLint("SetTextI18n")
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
        if (prefs.showDebugInfo) {
            binding.debugStats.apply {
                text =
                    "memcache: %d items\nmemcache bounds: %s\ndb queries: %d\nlast query: %dms".format(
                        cacheSize,
                        viewportInfo,
                        db().place.selectMerchantsByBoundsCallCount,
                        db().place.selectMerchantsByBoundsLastCallDurationMs,
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

    companion object {
        private const val MIN_QUERY_LENGTH = 3

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }

        const val EMPTY_GEOJSON = """{"type":"FeatureCollection","features":[]}"""
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