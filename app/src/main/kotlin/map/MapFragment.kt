package map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import element.ElementFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentMapBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import search.SearchResultModel

class MapFragment : Fragment() {

    companion object {
        private const val DEFAULT_MAP_ZOOM = 12f
    }

    private val model: MapModel by viewModel()

    private val searchResultModel: SearchResultModel by sharedViewModel()

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
            val baseTopMargin =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8f,
                    resources.displayMetrics,
                ).toInt()

            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())

            view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                topMargin = insets.top + baseTopMargin
            }

            bottomSheetBehavior.expandedOffset = insets.top

            WindowInsetsCompat.CONSUMED
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.setArgs(requireContext())

        binding.search.setOnClickListener {
            val action = MapFragmentDirections.actionMapFragmentToSearchFragment(
                model.userLocation.value.latitude.toString(),
                model.userLocation.value.longitude.toString(),
            )

            findNavController().navigate(action)
        }

        binding.donate.setOnClickListener {
            findNavController().navigate(MapFragmentDirections.actionMapFragmentToDonationFragment())
        }

        binding.actions.setOnClickListener {
            val popup = PopupMenu(requireContext(), binding.actions)

            popup.apply {
                menuInflater.inflate(R.menu.search, menu)

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_add_element -> {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data =
                                Uri.parse("https://btcmap.org/add-location")
                            startActivity(intent)
                        }
                        R.id.action_settings -> {
                            findNavController().navigate(MapFragmentDirections.actionMapFragmentToSettingsFragment())
                        }
                    }

                    true
                }
            }.show()
        }

        binding.map.apply {
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 5.0
            setMultiTouchControls(true)
            addLocationOverlay()
            addCancelSelectionOverlay()
            addViewportListener()
        }

        model.invalidateMarkersCache()

        bottomSheetBehavior = BottomSheetBehavior.from(binding.elementDetails)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addSlideCallback()

        viewLifecycleOwner.lifecycleScope.launch {
            val elementDetailsToolbar = getElementDetailsToolbar()
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
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermissions()
                return@setOnClickListener
            }

            val mapController = binding.map.controller
            mapController.setZoom(DEFAULT_MAP_ZOOM.toDouble())
            mapController.setCenter(model.userLocation.value)
        }

        searchResultModel.element.filterNotNull().onEach {
            val mapController = binding.map.controller
            mapController.setZoom(DEFAULT_MAP_ZOOM.toDouble())
            val startPoint = GeoPoint(it.lat, it.lon)
            mapController.setCenter(startPoint)
            model.selectElement(it, true)
            searchResultModel.element.update { null }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        var elementsOverlay: RadiusMarkerClusterer? = null

        viewLifecycleOwner.lifecycleScope.launch {
            model.visibleElements.collectLatest { elementWithMarkers ->
                if (elementsOverlay != null) {
                    binding.map.overlays.remove(elementsOverlay)
                }

                elementsOverlay = RadiusMarkerClusterer(requireContext())

                val clusterIcon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_cluster)!!
                DrawableCompat.setTint(
                    clusterIcon,
                    requireContext().getPrimaryContainerColor(model.conf.conf.value)
                )

                val pinSizePx =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        48f,
                        resources.displayMetrics,
                    ).toInt()
                elementsOverlay!!.setIcon(clusterIcon.toBitmap(pinSizePx, pinSizePx))
                elementsOverlay!!.textPaint.color =
                    requireContext().getOnPrimaryContainerColor(model.conf.conf.value)

                elementWithMarkers.forEach {
                    val marker = Marker(binding.map)
                    marker.position = GeoPoint(it.element.lat, it.element.lon)
                    marker.icon = it.marker

                    marker.setOnMarkerClickListener { _, _ ->
                        model.selectElement(it.element, false)
                        true
                    }

                    elementsOverlay!!.add(marker)
                }

                binding.map.overlays.add(elementsOverlay)
                binding.map.invalidate()
            }
        }

        var ignoreNextLocation = false

        model.mapBoundingBox.value?.let {
            ignoreNextLocation = true

            viewLifecycleOwner.lifecycleScope.launch {
                while (binding.map.getIntrinsicScreenRect(null).height() == 0) {
                    delay(10)
                }

                binding.map.zoomToBoundingBox(model.mapBoundingBox.value, false)
            }
        }

        model.moveToLocation.onEach {
            if (ignoreNextLocation) {
                ignoreNextLocation = false
                return@onEach
            }

            val mapController = binding.map.controller
            mapController.setZoom(DEFAULT_MAP_ZOOM.toDouble())
            mapController.setCenter(it)
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        lifecycleScope.launchWhenResumed {
            val toast = Toast.makeText(
                requireContext(),
                "",
                Toast.LENGTH_LONG,
            )

            model.syncMessage.collectLatest {
                toast.setText(it)

                if (it.isNotBlank()) {
                    toast.show()
                } else {
                    toast.cancel()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.syncElements()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
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

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars = !darkMap
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
                model.selectElement(null, false)
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
                model.setMapViewport(binding.map.boundingBox)
                return false
            }

            override fun onZoom(event: ZoomEvent?) = false
        })
    }

    private fun BottomSheetBehavior<*>.addSlideCallback() {
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    model.selectElement(null, false)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.fab.isVisible = slideOffset < 0.5f
                (childFragmentManager.findFragmentById(R.id.elementFragment) as ElementFragment).setScrollProgress(
                    slideOffset
                )
            }
        })
    }

    private suspend fun getElementDetailsToolbar(): Toolbar {
        while (elementFragment.view == null
            || elementFragment.requireView().findViewById<View>(R.id.toolbar)!!.height == 0
        ) {
            delay(10)
        }

        return elementFragment.requireView().findViewById(R.id.toolbar)!!
    }
}