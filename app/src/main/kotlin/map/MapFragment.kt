package map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.sqlite.SQLiteConnection
import api.Api
import app.dpToPx
import area.AreaResultModel
import area.bounds
import com.google.android.material.bottomsheet.BottomSheetBehavior
import element.ElementFragment
import element.ElementsCluster
import element.ElementsRepo
import element_comment.ElementCommentRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.R
import org.btcmap.databinding.FragmentMapBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.OnCameraIdleListener
import org.maplibre.android.maps.Style
import search.SearchAdapter
import search.SearchModel
import search.SearchResultModel
import settings.MapStyle
import settings.SettingsFragment
import settings.mapStyle
import settings.mapViewport
import settings.markerBackgroundColor
import settings.markerIconColor
import settings.prefs
import settings.uri
import java.time.ZoneOffset
import java.time.ZonedDateTime

class MapFragment : Fragment() {

    private val model: MapModel by viewModel()
    private val searchModel: SearchModel by viewModel()

    private val searchResultModel: SearchResultModel by activityViewModel()
    private val areaResultModel: AreaResultModel by activityViewModel()

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val elementFragment by lazy {
        childFragmentManager.findFragmentById(R.id.elementFragment) as ElementFragment
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
                    LocationComponentOptions.builder(requireContext())
                        .pulseEnabled(true)
                        .build()
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
        style: Style,
        locationComponentOptions: LocationComponentOptions
    ): LocationComponentActivationOptions {
        return LocationComponentActivationOptions
            .builder(requireContext(), style)
            .locationComponentOptions(locationComponentOptions)
            .useDefaultLocationEngine(true)
            .locationEngineRequest(
                LocationEngineRequest.Builder(750)
                    .setFastestInterval(750)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build()
            )
            .build()
    }

    private var emptyClusterBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.placeTypeSwitch) { v, windowInsets ->
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

        emptyClusterBitmap = null

        val markersRepo = MapMarkersRepo(requireContext())

        binding.filterMerchantsInactive.setOnClickListener {
            binding.filterMerchantsActive.isVisible = true
            binding.filterMerchantsInactive.isVisible = false

            binding.filterEventsActive.isVisible = false
            binding.filterEventsInactive.isVisible = true

            binding.filterExchangesActive.isVisible = false
            binding.filterExchangesInactive.isVisible = true

            binding.map.getMapAsync { map ->
                model.loadItems(
                    bounds = map.projection.visibleRegion.latLngBounds,
                    zoom = map.cameraPosition.zoom,
                    filter = MapModel.Filter.Merchants,
                )
            }
        }

        binding.filterEventsInactive.setOnClickListener {
            binding.filterMerchantsActive.isVisible = false
            binding.filterMerchantsInactive.isVisible = true

            binding.filterEventsActive.isVisible = true
            binding.filterEventsInactive.isVisible = false

            binding.filterExchangesActive.isVisible = false
            binding.filterExchangesInactive.isVisible = true

            binding.map.getMapAsync { map ->
                model.loadItems(
                    bounds = map.projection.visibleRegion.latLngBounds,
                    zoom = map.cameraPosition.zoom,
                    filter = MapModel.Filter.Events,
                )
            }
        }

        binding.filterExchangesInactive.setOnClickListener {
            binding.filterMerchantsActive.isVisible = false
            binding.filterMerchantsInactive.isVisible = true

            binding.filterEventsActive.isVisible = false
            binding.filterEventsInactive.isVisible = true

            binding.filterExchangesActive.isVisible = true
            binding.filterExchangesInactive.isVisible = false

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
                R.id.action_add_element -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://btcmap.org/add-location")
                    startActivity(intent)
                }

                R.id.action_settings -> {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<SettingsFragment>(R.id.nav_host_fragment)
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
                        val browserIntent =
                            Intent(Intent.ACTION_VIEW, url.toString().toUri())
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
                elementFragment.setElement(it)
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

        val visibleElements = mutableListOf<Marker>()

        viewLifecycleOwner.lifecycleScope.launch {
            model.items.collectLatest { newElements ->
                binding.map.getMapAsync { map ->
                    visibleElements.forEach { map.removeMarker(it) }
                    visibleElements.clear()
                    newElements.forEach {
                        when (it) {
                            is MapModel.MapItem.ElementsCluster -> {
                                val marker =
                                    MarkerOptions().position(LatLng(it.cluster.lat, it.cluster.lon))

                                if (it.cluster.count == 1L) {
                                    val icon = if (it.cluster.requiresCompanionApp) {
                                        markersRepo.getWarningMarker(it.cluster.iconId.ifBlank { "question_mark" })
                                    } else if (it.cluster.boostExpires != null && it.cluster.boostExpires.isAfter(
                                            ZonedDateTime.now(
                                                ZoneOffset.UTC
                                            )
                                        )
                                    ) {
                                        markersRepo.getBoostedMarker(
                                            it.cluster.iconId.ifBlank { "question_mark" },
                                            it.cluster.comments,
                                        )
                                    } else {
                                        requireContext().marker(
                                            iconId = it.cluster.iconId.ifBlank { "question_mark" },
                                            backgroundColor = prefs.markerBackgroundColor(
                                                requireContext()
                                            ),
                                            iconColor = prefs.markerIconColor(requireContext()),
                                            counter = it.cluster.comments,
                                        )
                                    }
                                    val newBitmap = Bitmap.createBitmap(
                                        icon.bitmap.width,
                                        icon.bitmap.height * 2,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = Canvas(newBitmap)
                                    canvas.drawBitmap(icon.bitmap, Matrix(), null)
                                    marker.icon(
                                        IconFactory.getInstance(requireContext())
                                            .fromBitmap(newBitmap)
                                    )
                                    marker.snippet(it.cluster.id.toString())
                                } else {
                                    val icon = createClusterIcon(it.cluster).toDrawable(resources)
                                    marker.icon(
                                        IconFactory.getInstance(requireContext())
                                            .fromBitmap(icon.bitmap)
                                    )
                                }

                                visibleElements += map.addMarker(marker)
                            }

                            is MapModel.MapItem.Event -> {
                                val marker =
                                    MarkerOptions().position(LatLng(it.event.lat, it.event.lon))
                                val icon = requireContext().marker(
                                    iconId = "event",
                                    backgroundColor = prefs.markerBackgroundColor(
                                        requireContext()
                                    ),
                                    iconColor = prefs.markerIconColor(requireContext()),
                                    counter = 0,
                                )
                                val newBitmap = Bitmap.createBitmap(
                                    icon.bitmap.width,
                                    icon.bitmap.height * 2,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = Canvas(newBitmap)
                                canvas.drawBitmap(icon.bitmap, Matrix(), null)
                                marker.icon(
                                    IconFactory.getInstance(requireContext())
                                        .fromBitmap(newBitmap)
                                )
                                marker.snippet("event:${it.event.website}")
                                visibleElements += map.addMarker(marker)
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val elementsRepo by inject<ElementsRepo>()

                if (elementsRepo.selectCount() == 0L && elementsRepo.hasBundledElements()) {
                    elementsRepo.fetchBundledElements()
                    refreshData()
                }

                elementsRepo.sync()
                refreshData()

                val api by inject<Api>()
                val conn by inject<SQLiteConnection>()
                val events = api.getEvents()
                withContext(Dispatchers.IO) { event.deleteAllAndInsertBatch(events, conn) }
                refreshData()

                val commentsRepo by inject<ElementCommentRepo>()
                commentsRepo.sync()
                refreshData()
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val pickedPlace = searchResultModel.element.value
                searchResultModel.element.update { null }

                val pickedArea = areaResultModel.area.value
                areaResultModel.area.update { null }

                if (pickedPlace != null) {
                    binding.map.getMapAsync {
                        model.selectElement(pickedPlace.id)
                        it.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    model.selectedElement.value!!.lat,
                                    model.selectedElement.value!!.lon
                                ), 16.0
                            )
                        )
                    }
                    return@repeatOnLifecycle
                }

                if (pickedArea != null) {
                    binding.map.getMapAsync {
                        val boundingBoxPaddingPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            16f,
                            resources.displayMetrics,
                        ).toInt()
                        it.moveCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                pickedArea.tags.bounds(),
                                boundingBoxPaddingPx
                            )
                        )
                    }
                    return@repeatOnLifecycle
                }
            }
        }

        val searchAdapter = SearchAdapter { row ->
            binding.searchView.clearText()
            binding.searchView.hide()

            model.selectElement(row.element.id)
            binding.map.getMapAsync {
                it.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            model.selectedElement.value!!.lat,
                            model.selectedElement.value!!.lon
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

        binding.searchView.editText.doAfterTextChanged {
            val text = it.toString()
            searchModel.setSearchString(text)
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
            prefs.mapViewport =
                map.projection.visibleRegion.latLngBounds

            refreshData()

            searchModel.setLocation(
                LatLng(
                    map.cameraPosition.target!!.latitude, map.cameraPosition.target!!.longitude
                )
            )
        }
    }

    private fun refreshData() {
        binding.map.getMapAsync { map ->
            viewLifecycleOwner.lifecycleScope.launch {
                val filter =
                    if (binding.filterMerchantsActive.isVisible) {
                        MapModel.Filter.Merchants
                    } else if (binding.filterEventsActive.isVisible) {
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
                    binding.fab.show()
                    binding.fab.isVisible = true
                } else {
                    binding.fab.isVisible = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                elementFragment.onPartialExpanded(slideOffset)
                Log.d("map", slideOffset.toString())
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

    private fun createClusterIcon(cluster: ElementsCluster): Bitmap {
        val pinSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 32f, requireContext().resources.displayMetrics
        ).toInt()

        if (emptyClusterBitmap == null) {
            val emptyClusterDrawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.cluster)!!
            DrawableCompat.setTint(
                emptyClusterDrawable, requireContext().getPrimaryContainerColor()
            )
            emptyClusterBitmap =
                emptyClusterDrawable.toBitmap(width = pinSizePx, height = pinSizePx)
        }

        val clusterIcon =
            createBitmap(emptyClusterBitmap!!.width, emptyClusterBitmap!!.height).applyCanvas {
                drawBitmap(emptyClusterBitmap!!, 0f, 0f, Paint())
            }

        clusterIcon.applyCanvas {
            val paint = Paint().apply {
                textSize = pinSizePx.toFloat() / 3
                color = requireContext().getOnPrimaryContainerColor()
            }

            val text = cluster.count.toString()
            val textWidth = paint.measureText(text)

            drawText(
                text,
                clusterIcon.width / 2f - textWidth / 2,
                clusterIcon.height / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2,
                paint
            )
        }

        return clusterIcon
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
}