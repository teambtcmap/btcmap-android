package map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import area.AreaResultModel
import area.polygons
import com.google.android.material.bottomsheet.BottomSheetBehavior
import element.ElementFragment
import element.ElementsCluster
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
import org.osmdroid.views.overlay.IconOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import search.SearchAdapter
import search.SearchModel
import search.SearchResultModel
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

    private val postNotificationsPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.searchBar) { view, windowInsets ->
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

        val markersRepo = MapMarkersRepo(requireContext())

        binding.searchBar.setOnMenuItemClickListener {
            val nav = findNavController()

            when (it.itemId) {
                R.id.action_donate -> nav.navigate(R.id.donationFragment)
                R.id.action_add_element -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://btcmap.org/add-location")
                    startActivity(intent)
                }

                R.id.action_delivery -> {
                    nav.navigate(
                        R.id.deliveryFragment,
                        bundleOf(
                            "userLat" to binding.map.boundingBox.centerLatitude.toFloat(),
                            "userLon" to binding.map.boundingBox.centerLongitude.toFloat(),
                            "searchAreaId" to 662L,
                        ),
                    )
                }

                R.id.action_areas -> {
                    nav.navigate(
                        R.id.areasFragment,
                        bundleOf(
                            "lat" to binding.map.boundingBox.centerLatitude.toFloat(),
                            "lon" to binding.map.boundingBox.centerLongitude.toFloat(),
                        ),
                    )
                }

                R.id.action_trends -> nav.navigate(R.id.reportsFragment)
                R.id.action_users -> nav.navigate(R.id.usersFragment)
                R.id.action_events -> nav.navigate(R.id.eventsFragment)
                R.id.action_settings -> nav.navigate(R.id.settingsFragment)
            }

            true
        }

        binding.map.apply {
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
            setScrollableAreaLimitDouble(
                BoundingBox(
                    TileSystemWebMercator.MaxLatitude,
                    TileSystemWebMercator.MaxLongitude,
                    TileSystemWebMercator.MinLatitude,
                    TileSystemWebMercator.MinLongitude,
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
                binding.map.controller.animateTo(
                    model.userLocation.value,
                    15.0,
                    null,
                )
            }
        }

        val visibleElements = mutableListOf<Marker>()

        viewLifecycleOwner.lifecycleScope.launch {
            model.conf.conf.map { it.showAtms }.collectLatest {
                if (it) {
                    model.setExcludedCategories(emptyList())
                } else {
                    model.setExcludedCategories(listOf("atm"))
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            model.conf.conf.map { it.showOsmAttribution }.collectLatest {
                binding.osmAttribution.isVisible = it
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            model.visibleElements.collectLatest { newElements ->
                visibleElements.forEach {
                    binding.map.overlays -= it
                }

                visibleElements.clear()

                newElements.forEach {
                    when (it) {
                        is MapModel.MapItem.ElementsCluster -> {
                            val marker = Marker(binding.map)
                            marker.position = GeoPoint(it.cluster.lat, it.cluster.lon)

                            if (it.cluster.count == 1L) {
                                val icon = if (it.cluster.requiresCompanionApp) {
                                    markersRepo.getWarningMarker(it.cluster.iconId.ifBlank { "question_mark" })
                                } else if (
                                    it.cluster.boostExpires != null
                                    && it.cluster.boostExpires.isAfter(ZonedDateTime.now(ZoneOffset.UTC))
                                ) {
                                    markersRepo.getBoostedMarker(it.cluster.iconId.ifBlank { "question_mark" })
                                } else {
                                    markersRepo.getMarker(it.cluster.iconId.ifBlank { "question_mark" })
                                }

                                marker.icon = icon
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            } else {
                                marker.icon = createClusterIcon(it.cluster).toDrawable(resources)
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }

                            marker.setOnMarkerClickListener { _, _ ->
                                if (it.cluster.count == 1L) {
                                    model.selectElement(it.cluster.id, false)
                                }

                                true
                            }

                            visibleElements += marker
                            binding.map.overlays += marker
                        }

                        is MapModel.MapItem.Meetup -> {
                            val marker = Marker(binding.map)
                            marker.position = GeoPoint(it.meetup.lat, it.meetup.lon)
                            marker.icon = markersRepo.meetupMarker
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.setOnMarkerClickListener { _, _ ->
                                findNavController().navigate(
                                    resId = R.id.areaFragment,
                                    args = bundleOf("area_id" to it.meetup.areaId),
                                )

                                true
                            }
                            visibleElements += marker
                            binding.map.overlays += marker
                        }
                    }
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
                    val boundingBox = boundingBox(pickedArea.tags.polygons())
                    val boundingBoxPaddingPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        16f,
                        resources.displayMetrics,
                    ).toInt()
                    binding.map.zoomToBoundingBox(boundingBox, false, boundingBoxPaddingPx)
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
                searchModel.setLocation(GeoPoint(binding.map.mapCenter))
                binding.map.addViewportListener()
            }
        }

        val searchAdapter = SearchAdapter { row ->
            binding.searchView.clearText()
            binding.searchView.hide()

            model.selectElement(row.element.id, true)
            val mapController = binding.map.controller
            mapController.setZoom(16.toDouble())
            val startPoint =
                GeoPoint(model.selectedElement.value!!.lat, model.selectedElement.value!!.lon)
            mapController.setCenter(startPoint)
            mapController.zoomTo(19.0)
        }

        binding.searchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResults.adapter = searchAdapter

        searchModel.searchResults
            .onEach {
                searchAdapter.submitList(it) {
                    val layoutManager = binding.searchResults.layoutManager ?: return@submitList
                    layoutManager.scrollToPosition(0)
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.searchView.editText.doAfterTextChanged {
            val text = it.toString()
            searchModel.setSearchString(text)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            postNotificationsPermissionRequest.launch(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            )
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

    private fun MapView.addLocationOverlay() {
        val iconSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f,
            requireContext().resources.displayMetrics,
        ).toInt()

        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.cluster)!!
            .toBitmap(width = iconSizePx, height = iconSizePx)
            .toDrawable(resources)

        DrawableCompat.setTint(
            icon,
            Color.parseColor("#4285f4"),
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var overlay: Overlay? = null

                model.userLocation.collect {
                    if (it != null) {
                        binding.map.overlays -= overlay
                        overlay = IconOverlay(it, icon)
                        binding.map.overlays += overlay
                        binding.map.invalidate()
                    }
                }
            }
        }
    }

    private fun MapView.addCancelSelectionOverlay() {
        overlays += MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                model.selectElement(0, false)
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
                searchModel.setLocation(GeoPoint(binding.map.mapCenter))
                return false
            }

            override fun onZoom(event: ZoomEvent?) = false
        })
    }

    private fun BottomSheetBehavior<*>.addSlideCallback() {
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    model.selectElement(0, false)
                    binding.fab.show()
                    binding.fab.isVisible = true
                } else {
                    binding.fab.isVisible = false
                }

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    requireActivity().window.statusBarColor = requireContext().getSurfaceColor()
                } else {
                    requireActivity().window.statusBarColor = Color.TRANSPARENT
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
                ContextCompat.getDrawable(requireContext(), R.drawable.cluster)!!
            DrawableCompat.setTint(
                emptyClusterDrawable,
                requireContext().getPrimaryContainerColor()
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
}