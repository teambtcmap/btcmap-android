package map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.location.Location
import android.os.Bundle
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
import androidx.core.view.ViewCompat
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
import bundle.BundledPlaces
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import db.db
import db.table.place.Place
import db.table.place.PlaceQueries
import place.PlaceFragment
import http.httpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.databinding.MapFragmentBinding
import org.json.JSONObject
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import place.isMerchant
import search.SearchAdapter
import settings.MapStyle
import settings.SettingsFragment
import settings.mapStyle
import settings.mapViewport
import settings.markerBackgroundColor
import settings.prefs
import settings.uri
import sync.CommentSync
import sync.EventSync
import sync.PlaceSync
import db.table.place.PlaceProjectionCluster
import fragment.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import search.SearchAdapterItem
import java.text.NumberFormat

class MapFragment : Fragment() {

    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private val placeFragment by lazy {
        childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private val insetsController: WindowInsetsControllerCompat? by lazy {
        activity?.window?.decorView?.let(ViewCompat::getWindowInsetsController)
    }

    private var backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

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

    private fun selectPlace(place: Place?) {
        if (place != null) {
            runBlocking { getPlaceDetailsToolbar() }
            placeFragment.setPlace(place)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initSearchBar(binding)

        launchUpdateChecker()

        binding.showMerchants.setOnClickListener { setFilter(Filter.MERCHANTS) }
        binding.showEvents.setOnClickListener { setFilter(Filter.EVENTS) }
        binding.showExchanges.setOnClickListener { setFilter(Filter.EXCHANGES) }

        binding.map.getMapAsync {
            it.addOnCameraIdleListener {
                prefs.mapViewport = it.projection.visibleRegion.latLngBounds
            }

            it.setStyle(Style.Builder().fromUri(prefs.mapStyle.uri(requireContext())))
            it.uiSettings.isCompassEnabled = false
            it.uiSettings.isRotateGesturesEnabled = false
            it.uiSettings.isLogoEnabled = false
            it.uiSettings.isAttributionEnabled = false
            it.addCancelSelectionOverlay()

            it.getStyle { style ->
                icon.init(requireContext(), style)
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

        setFilter(Filter.MERCHANTS)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.sync.isVisible = true

                if (BundledPlaces.import(requireContext(), db)) {
                    reloadData()
                }

                if (PlaceSync.run(db).rowsAffected > 0) {
                    reloadData()
                }

                if (EventSync.run(db).rowsAffected > 0) {
                    reloadData()
                }

                if (CommentSync.run(db).rowsAffected > 0) {
                    reloadData()
                }

                binding.sync.isVisible = false
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, backPressedCallback
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                binding.map.getMapAsync {
                    it.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            prefs.mapViewport, 0
                        )
                    )
                }
            }
        }

        val searchAdapter = SearchAdapter { row ->
            binding.searchView.clearText()
            binding.searchView.hide()

            val place = PlaceQueries.selectById(row.placeId, db)

            if (place.isMerchant()) {
                binding.showMerchants.performClick()
            } else {
                binding.showExchanges.performClick()
            }

            selectPlace(place)

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

    private fun MapLibreMap.addCancelSelectionOverlay() {
        addOnMapClickListener {
            selectPlace(null)
            true
        }
    }

    private fun BottomSheetBehavior<*>.addSlideCallback() {
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    selectPlace(null)
                    binding.fab.isVisible = true
                } else {
                    binding.fab.isVisible = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                placeFragment.onSlide(slideOffset)
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
        when (prefs.mapStyle) {
            MapStyle.Auto -> {
                val nightMode =
                    requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                insetsController?.isAppearanceLightStatusBars = !nightMode
            }

            MapStyle.Dark -> insetsController?.isAppearanceLightStatusBars = false
            else -> insetsController?.isAppearanceLightStatusBars = true
        }
    }

    fun restoreDefaultStatusBar() {
        val nightMode =
            requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        insetsController?.isAppearanceLightStatusBars = !nightMode
    }

    private fun List<PlaceProjectionCluster>.toGeoJson(): String {
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
                    "count": ${place.count},
                    "iconId": "${place.iconId}",
                    "requiresCompanionApp": ${place.requiresCompanionApp},
                    "comments": ${place.comments}
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
        reloadData()
    }

    private fun reloadData() {
        when (filter) {
            Filter.MERCHANTS -> reloadMerchants()
            Filter.EVENTS -> reloadEvents()
            Filter.EXCHANGES -> reloadExchanges()
        }
    }

    private var merchantsSource = GeoJsonSource(
        "merchantsSource",
        """{"type":"FeatureCollection","features":[]}""",
        GeoJsonOptions()
            .withCluster(true)
            .withClusterMaxZoom(14)
            .withClusterRadius(50)
    )

    private val merchantsClusterBackgroundLayer by lazy {
        CircleLayer("merchantsClusterBackground", merchantsSource.id).apply {
            setProperties(
                PropertyFactory.circleColor(prefs.markerBackgroundColor(requireContext())),
                PropertyFactory.circleRadius(18f),
            )
            val pointCount = Expression.toNumber(Expression.get("point_count"))
            setFilter(
                Expression.all(
                    Expression.has("point_count"),
                    Expression.gte(
                        pointCount,
                        Expression.literal(1)
                    )
                )
            )
        }
    }

    private val merchantsClusterCountLayer = SymbolLayer("merchantsClusterCount", merchantsSource.id).apply {
        setProperties(
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
            PropertyFactory.textSize(12f),
            PropertyFactory.textColor(Color.WHITE),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true)
        )
    }

    private val unclusteredMerchantsLayer = SymbolLayer("unclusteredMerchants", merchantsSource.id).apply {
        setProperties(
            PropertyFactory.iconImage("btcmap-marker"),
            PropertyFactory.iconAnchor(Expression.literal("bottom")),
            PropertyFactory.iconAllowOverlap(true), // Important
            PropertyFactory.iconIgnorePlacement(true) // Important
        )
        setFilter(
            Expression.neq(Expression.get("cluster"), true)
        )
    }

    private val unclusteredMerchantsCategoryIconsLayer = SymbolLayer("unclusteredMerchantsCategoryIcons", merchantsSource.id).apply {
        setProperties(
            PropertyFactory.iconImage(
                Expression.match(
                    Expression.get("iconId"),
                    *icon.matcher().toTypedArray()
                )
            ),
            PropertyFactory.iconAnchor(ICON_ANCHOR_CENTER),
            PropertyFactory.iconOffset(
                arrayOf(
                    0f,
                    -29f
                )
            ),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )
        setFilter(Expression.neq(Expression.get("cluster"), true))
    }

    private fun reloadMerchants() {
        val merchants = PlaceQueries.selectWithoutClustering(
            minLat = -90.0,
            maxLat = 90.0,
            minLon = -180.0,
            maxLon = 180.0,
            includeMerchants = true,
            includeExchanges = false,
            db,
        )

        binding.map.getMapAsync { map ->
            map.getStyle { style ->
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

                if (!style.sources.contains(merchantsSource)) {
                    style.addSource(merchantsSource)
                }

                if (style.getLayer(unclusteredMerchantsLayer.id) == null) {
                    style.addLayer(unclusteredMerchantsLayer)
                }

                if (style.getLayer(unclusteredMerchantsCategoryIconsLayer.id) == null) {
                    style.addLayer(unclusteredMerchantsCategoryIconsLayer)
                }

                if (style.getLayer(merchantsClusterBackgroundLayer.id) == null) {
                    style.addLayer(merchantsClusterBackgroundLayer)
                }

                if (style.getLayer(merchantsClusterCountLayer.id) == null) {
                    style.addLayer(merchantsClusterCountLayer)
                }

                merchantsSource.setGeoJson(merchants.toGeoJson())
            }
        }
    }

    private fun reloadEvents() {

    }

    private fun reloadExchanges() {

    }

    private fun launchUpdateChecker() {
        viewLifecycleOwner.lifecycleScope.launch {
            withResumed {
                binding.update.isVisible = false

                launch {
                    try {
                        val latestVerJson = httpClient.newCall(
                            Request.Builder()
                                .url("https://static.btcmap.org/android/latest-app-ver".toHttpUrl())
                                .build()
                        ).executeAsync().body.string().trim()

                        val latestVer = JSONObject(latestVerJson)
                        val latestVerCode = latestVer.getInt("code")
                        val latestVerName = latestVer.getString("name")
                        val latestVerUrl = latestVer.getString("url")

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

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }

    private val _searchResults = MutableStateFlow<List<SearchAdapterItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    suspend fun search(referenceLocation: LatLng, searchString: String) {
        if (searchString.length < MIN_QUERY_LENGTH) {
            _searchResults.update { emptyList() }
        } else {
            val unsortedPlaces = withContext(Dispatchers.IO) {
                PlaceQueries.selectBySearchString(searchString, db)
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

    private fun initInsets(binding: MapFragmentBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.fab) { v, windowInsets ->
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
}