package map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
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
import app.App
import bundle.BundledPlaces
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import db.db
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
import org.maplibre.android.maps.MapLibreMap.OnCameraIdleListener
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import place.isMerchant
import search.SearchAdapter
import search.SearchModel
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
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions

class MapFragment : Fragment() {

    private val model = MapModel()

    private val searchModel: SearchModel by lazy {
        SearchModel(
            requireContext().applicationContext as App
        )
    }

    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private val elementFragment by lazy {
        childFragmentManager.findFragmentById(R.id.elementFragment) as PlaceFragment
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

    fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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

    private var emptyClusterBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = MapFragmentBinding.inflate(inflater, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fab) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                rightMargin = insets.right + v.context.dpToPx(24)
                bottomMargin = insets.bottom + v.context.dpToPx(24)
                leftMargin = insets.left
            }

            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.buttonGroup) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                rightMargin = insets.right
                bottomMargin = insets.bottom + v.context.dpToPx(20)
                leftMargin = insets.left + v.context.dpToPx(24)
            }

            WindowInsetsCompat.CONSUMED
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.map.getMapAsync {
            it.addOnCameraIdleListener(onCameraIdleListener)
        }

        binding.update.iconColor(requireContext().getErrorColor())

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

                                binding.update.setOnClickListener {
                                    MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.update_available)
                                        .setMessage(
                                            getString(
                                                R.string.update_available_description,
                                                BuildConfig.VERSION_NAME, latestVerName
                                            )
                                        )
                                        .setPositiveButton(R.string.get_apk) { dialog, which ->
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

        emptyClusterBitmap = null

        binding.showMerchants.setOnClickListener {
            binding.showMerchants.isSelected = true
            binding.showEvents.isSelected = false
            binding.showExchanges.isSelected = false

            binding.map.getMapAsync { map ->
                model.loadItems(
                    bounds = map.projection.visibleRegion.latLngBounds,
                    zoom = map.cameraPosition.zoom,
                    filter = MapModel.Filter.Merchants,
                )
            }
        }

        binding.showEvents.setOnClickListener {
            binding.showMerchants.isSelected = false
            binding.showEvents.isSelected = true
            binding.showExchanges.isSelected = false

            binding.map.getMapAsync { map ->
                model.loadItems(
                    bounds = map.projection.visibleRegion.latLngBounds,
                    zoom = map.cameraPosition.zoom,
                    filter = MapModel.Filter.Events,
                )
            }
        }

        binding.showExchanges.setOnClickListener {
            binding.showMerchants.isSelected = false
            binding.showEvents.isSelected = false
            binding.showExchanges.isSelected = true

            binding.map.getMapAsync { map ->
                model.loadItems(
                    bounds = map.projection.visibleRegion.latLngBounds,
                    zoom = map.cameraPosition.zoom,
                    filter = MapModel.Filter.Exchanges,
                )
            }
        }

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

        binding.map.getMapAsync {
            it.setStyle(Style.Builder().fromUri(prefs.mapStyle.uri(requireContext())))
            it.uiSettings.isCompassEnabled = false
            it.uiSettings.isRotateGesturesEnabled = false
            it.uiSettings.isLogoEnabled = false
            it.uiSettings.isAttributionEnabled = false
            it.addCancelSelectionOverlay()
            it.setOnMarkerClickListener { marker ->
                val snippet = marker.snippet
                if (!snippet.isNullOrBlank()) {
                    if (snippet.startsWith("event:")) {
                        val url = snippet.replace("event:", "").toHttpUrl()
                        val browserIntent = Intent(Intent.ACTION_VIEW, url.toString().toUri())
                        startActivity(browserIntent)
                    } else {
                        val id = marker.snippet.toLong()
                        model.selectElement(id)
                    }
                }
                true
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.elementDetails)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addSlideCallback()

        viewLifecycleOwner.lifecycleScope.launch {
            val elementDetailsToolbar = getElementDetailsToolbar() ?: return@launch
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

        model.selectedElement.onEach {
            if (it != null) {
                getElementDetailsToolbar()
                elementFragment.setPlace(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

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

                val source = GeoJsonSource(
                    "places-source",
                    merchants.toGeoJson(),
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                )
                style.addSource(source)

                val unclustered = SymbolLayer("unclustered-points", "places-source")
                unclustered.setProperties(
                    PropertyFactory.iconImage("btcmap-marker"),
                    PropertyFactory.iconAnchor(Expression.literal("bottom")),
                    PropertyFactory.iconAllowOverlap(true), // Important
                    PropertyFactory.iconIgnorePlacement(true) // Important
                )
                unclustered.setFilter(
                    Expression.neq(Expression.get("cluster"), true)
                )
                style.addLayer(unclustered)

                val iconMapping = icon.init(requireContext(), style)

                val categoryIcons = SymbolLayer("category-icons", "places-source").apply {
                    setProperties(
                        PropertyFactory.iconImage(
                            Expression.match(
                                Expression.get("iconId"),
                                *iconMapping.toTypedArray()
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
                style.addLayer(categoryIcons)

                val circles = CircleLayer("merchant-clusters", "places-source")
                circles.setProperties(
                    PropertyFactory.circleColor(prefs.markerBackgroundColor(requireContext())),
                    PropertyFactory.circleRadius(18f),
                )
                val pointCount = Expression.toNumber(Expression.get("point_count"))
                circles.setFilter(
                    Expression.all(
                        Expression.has("point_count"),
                        Expression.gte(
                            pointCount,
                            Expression.literal(1)
                        )
                    )
                )
                style.addLayer(circles)

                val count = SymbolLayer("count", "places-source")
                count.setProperties(
                    PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                    PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                    PropertyFactory.textSize(12f),
                    PropertyFactory.textColor(Color.WHITE),
                    PropertyFactory.textIgnorePlacement(true),
                    PropertyFactory.textAllowOverlap(true)
                )
                style.addLayer(count)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.sync.isVisible = true

                if (BundledPlaces.import(requireContext(), db)) {
                    refreshData()
                }

                if (PlaceSync.run(db).rowsAffected > 0) {
                    refreshData()
                }

                if (EventSync.run(db).rowsAffected > 0) {
                    refreshData()
                }

                if (CommentSync.run(db).rowsAffected > 0) {
                    refreshData()
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

            model.selectElement(row.placeId)

            binding.map.getMapAsync {
                it.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            model.selectedElement.value!!.lat, model.selectedElement.value!!.lon
                        ), 16.0
                    )
                )
            }
        }

        binding.searchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResults.adapter = searchAdapter

        searchModel.searchResults.onEach {
            searchAdapter.submitList(it) {
                val layoutManager = binding.searchResults.layoutManager ?: return@submitList
                layoutManager.scrollToPosition(0)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        binding.searchView.editText.doAfterTextChanged { searchString ->
            binding.map.getMapAsync { map ->
                viewLifecycleOwner.lifecycleScope.launch {
                    searchModel.search(
                        referenceLocation = map.projection.visibleRegion.latLngBounds.center,
                        searchString = searchString.toString(),
                    )
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        binding.map.getMapAsync { it.removeOnCameraIdleListener(onCameraIdleListener) }
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
            model.selectElement(0)
            true
        }
    }

    private val onCameraIdleListener = OnCameraIdleListener {
        binding.map.getMapAsync { map ->
            prefs.mapViewport = map.projection.visibleRegion.latLngBounds
            refreshData()
        }
    }

    private fun refreshData() {
        binding.map.getMapAsync { map ->
            viewLifecycleOwner.lifecycleScope.launch {
                val filter = if (binding.showMerchants.isSelected) {
                    MapModel.Filter.Merchants
                } else if (binding.showEvents.isSelected) {
                    MapModel.Filter.Events
                } else {
                    MapModel.Filter.Exchanges
                }

                model.loadItems(
                    bounds = map.projection.visibleRegion.latLngBounds,
                    zoom = map.cameraPosition.zoom,
                    filter = filter,
                )
            }
        }
    }

    private fun BottomSheetBehavior<*>.addSlideCallback() {
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    model.selectElement(0)
                    binding.fab.isVisible = true
                } else {
                    binding.fab.isVisible = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                elementFragment.onSlide(slideOffset)
            }
        })
    }

    private suspend fun getElementDetailsToolbar(): Toolbar? {
        var attempts = 0

        while (elementFragment.view == null || elementFragment.requireView()
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

        return elementFragment.requireView().findViewById(R.id.toolbar)!!
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
}