package map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import areas.AreaResultModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import element.ElementFragment
import elements.ElementsCluster
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.R
import org.btcmap.databinding.FragmentMapBinding
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystemWebMercator
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import search.SearchResultModel

class MapFragment : Fragment() {

    private val model: MapModel by viewModel()

    private val searchResultModel: SearchResultModel by activityViewModel()
    private val areaResultModel: AreaResultModel by activityViewModel()

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val elementFragment by lazy {
        childFragmentManager.findFragmentById(R.id.elementFragment) as ElementFragment
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private var backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (it.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            model.onLocationPermissionGranted()
        }
    }

    private var emptyClusterBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext()),
        )

        _binding = FragmentMapBinding.inflate(inflater, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.search) { view, windowInsets ->
            val baseTopMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics,
            ).toInt()

            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())

            view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                topMargin = insets.top + baseTopMargin
            }

            bottomSheetBehavior.expandedOffset = insets.top

            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.osmAttribution.translationY = -navBarsInsets.bottom.toFloat()
            binding.fab.translationY = -navBarsInsets.bottom.toFloat()

            WindowInsetsCompat.CONSUMED
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emptyClusterBitmap = null

        val markersRepo = MapMarkersRepo(requireContext(), model.conf)

        binding.search.setOnClickListener {
            findNavController().navigate(
                R.id.searchFragment,
                bundleOf(
                    "lat" to binding.map.boundingBox.centerLatitude.toFloat(),
                    "lon" to binding.map.boundingBox.centerLongitude.toFloat(),
                ),
            )
        }

        binding.donate.setOnClickListener {
            findNavController().navigate(R.id.donationFragment)
        }

        binding.actions.setOnClickListener {
            val popup = PopupMenu(requireContext(), binding.actions)

            popup.apply {
                menuInflater.inflate(R.menu.search, menu)

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_add_element -> {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse("https://btcmap.org/add-location")
                            startActivity(intent)
                        }
                        R.id.action_trends -> {
                            findNavController().navigate(
                                R.id.reportsFragment,
                                bundleOf("area_id" to ""),
                            )
                        }
                        R.id.action_areas -> {
                            findNavController().navigate(
                                R.id.areasFragment,
                                bundleOf(
                                    "lat" to binding.map.boundingBox.centerLatitude.toFloat(),
                                    "lon" to binding.map.boundingBox.centerLongitude.toFloat(),
                                ),
                            )
                        }
                        R.id.action_element_events -> {
                            findNavController().navigate(R.id.eventsFragment)
                        }
                        R.id.action_users -> {
                            findNavController().navigate(R.id.usersFragment)
                        }
                        R.id.action_settings -> {
                            findNavController().navigate(R.id.settingsFragment)
                        }
                    }

                    true
                }
            }.show()
        }

        binding.map.apply {
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
            setScrollableAreaLimitDouble(
                BoundingBox(
                    TileSystemWebMercator.MaxLatitude,
                    TileSystemWebMercator.MaxLongitude,
                    TileSystemWebMercator.MinLatitude,
                    TileSystemWebMercator.MinLongitude
                )
            )
            setMultiTouchControls(true)
            addLocationOverlay()
            addCancelSelectionOverlay()
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

        binding.fab.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermissions()
                return@setOnClickListener
            }

            val userLocation = model.userLocation.value

            if (userLocation != null) {
                binding.map.controller.setCenter(model.userLocation.value)
            }
        }

        val visibleElements = mutableListOf<Marker>()

        viewLifecycleOwner.lifecycleScope.launch {
            model.visibleElements.collectLatest { newElements ->
                visibleElements.forEach {
                    binding.map.overlays -= it
                }

                visibleElements.clear()

                newElements.forEach {
                    val marker = Marker(binding.map)
                    marker.position = GeoPoint(it.lat, it.lon)

                    if (it.count == 1L) {
                        marker.icon = markersRepo.getMarker(it.iconId.ifBlank { "question_mark" })
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    } else {
                        marker.icon = createClusterIcon(it).toDrawable(resources)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }

                    marker.setOnMarkerClickListener { _, _ ->
                        if (it.count == 1L) {
                            model.selectElement(it.id, false)
                        }

                        true
                    }

                    visibleElements += marker
                    binding.map.overlays += marker
                }

                binding.map.invalidate()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.syncElements()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, backPressedCallback
        )

        val nightMode =
            resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val darkMap = nightMode && model.conf.conf.value.darkMap

        if (darkMap) {
            binding.map.overlayManager.tilesOverlay.apply {
                setColorFilter(TilesOverlay.INVERT_COLORS)
                loadingBackgroundColor = android.R.color.black
                loadingLineColor = Color.argb(255, 0, 255, 0)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var attempts = 0

                while (binding.map.getIntrinsicScreenRect(null).height() == 0) {
                    if (attempts >= 100) {
                        requireActivity().finish()
                        return@repeatOnLifecycle
                    } else {
                        attempts++
                        delay(10)
                    }
                }

                val pickedPlace = searchResultModel.element.value
                searchResultModel.element.update { null }

                val pickedArea = areaResultModel.area.value
                areaResultModel.area.update { null }

                if (pickedPlace != null) {
                    binding.map.addViewportListener()
                    val mapController = binding.map.controller
                    mapController.setZoom(16.toDouble())
                    val startPoint =
                        GeoPoint(pickedPlace.lat, pickedPlace.lon)
                    mapController.setCenter(startPoint)
                    binding.map.post {
                        mapController.zoomTo(19.0)
                    }
                    model.selectElement(pickedPlace.id, true)
                    return@repeatOnLifecycle
                }

                if (pickedArea != null) {
                    binding.map.addViewportListener()

                    binding.map.zoomToBoundingBox(
                        BoundingBox(
                            pickedArea.tags["box:north"]!!.jsonPrimitive.double,
                            pickedArea.tags["box:east"]!!.jsonPrimitive.double,
                            pickedArea.tags["box:south"]!!.jsonPrimitive.double,
                            pickedArea.tags["box:west"]!!.jsonPrimitive.double,
                        ), false
                    )
                    return@repeatOnLifecycle
                }

                val firstBoundingBox = model.mapViewport.firstOrNull()?.boundingBox
                binding.map.zoomToBoundingBox(firstBoundingBox, false)
                model.setMapViewport(
                    MapModel.MapViewport(
                        binding.map.zoomLevelDouble,
                        binding.map.boundingBox,
                    )
                )
                binding.map.addViewportListener()
            }
        }

//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
//                val bounds = loadAreaBounds("de")
//                Toast.makeText(requireContext(), "Got ${bounds.size} polygons", Toast.LENGTH_SHORT).show()
//
//                bounds.forEach {
//                    val poly = Polygon(binding.map)
//                    poly.fillColor = Color.parseColor("#88f7931a")
//                    poly.strokeWidth = 1f
//                    poly.points = it.map { GeoPoint(it.second, it.first) }
//                    binding.map.overlays.add(poly)
//                    binding.map.invalidate()
//                }
//            }
//        }
    }

//    private suspend fun loadAreaBounds(id: String): List<List<Pair<Double, Double>>> {
//        val url = "https://data.btcmap.org/areas/$id.json"
//        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
//        val response = runCatching { request.await() }.getOrNull()
//            ?: return emptyList()
//
//        val area: JsonObject = Json.decodeFromString(response.body!!.string())
//        val geometry: JsonObject = area["geometry"]!!.jsonObject
//        val coordinates: JsonArray = geometry["coordinates"]!!.jsonArray
//
//        return coordinates.map { polygon ->
//            polygon.jsonArray[0].jsonArray.map {
//                Pair(it.jsonArray[0].jsonPrimitive.double, it.jsonArray[1].jsonPrimitive.double)
//            }
//        }
//    }

    override fun onResume() {
        super.onResume()

        val nightMode =
            resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val darkMap = nightMode && model.conf.conf.value.darkMap

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars = !darkMap
    }

    override fun onPause() {
        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars =
            when (requireContext().resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_NO -> true
                else -> false
            }

        super.onPause()
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

    private fun MapView.addLocationOverlay() {
        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), this)
        locationOverlay.enableMyLocation()
        binding.map.overlays += locationOverlay
    }

    private fun MapView.addCancelSelectionOverlay() {
        overlays += MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                model.selectElement("", false)
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })
    }

    private fun MapView.addViewportListener() {
        addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                model.setMapViewport(
                    MapModel.MapViewport(
                        binding.map.zoomLevelDouble,
                        binding.map.boundingBox,
                    )
                )
                return false
            }

            override fun onZoom(event: ZoomEvent?) = false
        })
    }

    private fun BottomSheetBehavior<*>.addSlideCallback() {
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    model.selectElement("", false)
                    binding.fab.show()
                    binding.fab.isVisible = true
                } else {
                    binding.fab.isVisible = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
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
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_cluster)!!
            DrawableCompat.setTint(
                emptyClusterDrawable,
                requireContext().getPrimaryContainerColor(model.conf.conf.value)
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
                color = requireContext().getOnPrimaryContainerColor(model.conf.conf.value)
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
}