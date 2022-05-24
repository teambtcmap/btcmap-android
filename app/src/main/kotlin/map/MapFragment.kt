package map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import search.PlacesSearchResultModel

class MapFragment : Fragment() {

    companion object {
        private const val DEFAULT_MAP_ZOOM = 12f
    }

    private val model: MapModel by viewModel()

    private val placesSearchResultModel: PlacesSearchResultModel by sharedViewModel()

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val placeDetailsFragment by lazy {
        childFragmentManager.findFragmentById(R.id.placeDetailsFragment) as PlaceDetailsFragment
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private var backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                model.onLocationPermissionGranted()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                model.onLocationPermissionGranted()
            }
            else -> {}
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.apply {
            inflateMenu(R.menu.map)

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_add -> {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://wiki.openstreetmap.org/wiki/How_to_contribute")
                        startActivity(intent)
                    }

                    R.id.action_donate -> {
                        findNavController().navigate(MapFragmentDirections.actionMapFragmentToDonationFragment())
                    }

                    R.id.action_search -> {
                        lifecycleScope.launch {
                            val action = MapFragmentDirections.actionMapFragmentToPlacesSearchFragment(
                                model.userLocation.value.lat.toString(),
                                model.userLocation.value.lon.toString(),
                            )

                            findNavController().navigate(action)
                        }
                    }
                }

                true
            }
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

        bottomSheetBehavior = BottomSheetBehavior.from(binding.placeDetails)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addSlideCallback()

        viewLifecycleOwner.lifecycleScope.launch {
            val placeDetailsToolbar = getPlaceDetailsToolbar()
            bottomSheetBehavior.peekHeight = placeDetailsToolbar.height

            placeDetailsToolbar.setOnClickListener {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
                }
            }
        }

        model.selectedPlace.onEach {
            if (it != null) {
                getPlaceDetailsToolbar()
                placeDetailsFragment.setPlace(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        binding.fab.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermissions()
                return@setOnClickListener
            }

            val location = model.userLocation.value
            val mapController = binding.map.controller
            mapController.setZoom(DEFAULT_MAP_ZOOM.toDouble())
            val startPoint = GeoPoint(location.lat, location.lon)
            mapController.setCenter(startPoint)
        }

        placesSearchResultModel.place
            .filterNotNull()
            .onEach {
                val mapController = binding.map.controller
                mapController.setZoom(DEFAULT_MAP_ZOOM.toDouble())
                val startPoint = GeoPoint(it.lat, it.lon)
                mapController.setCenter(startPoint)
                model.selectPlace(it.id, true)
                placesSearchResultModel.place.update { null }
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        var placesOverlay: RadiusMarkerClusterer? = null

        viewLifecycleOwner.lifecycleScope.launch {
            model.visiblePlaces.collectLatest { placeWithMarkers ->
                if (placesOverlay != null) {
                    binding.map.overlays.remove(placesOverlay)
                }

                placesOverlay = RadiusMarkerClusterer(context!!)
                val clusterIcon = ContextCompat.getDrawable(context!!, R.drawable.ic_cluster)!!
                val pinSizePx =
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 128f, resources.displayMetrics).toInt()
                placesOverlay!!.setIcon(clusterIcon.toBitmap(pinSizePx, pinSizePx))

                placeWithMarkers.forEach {
                    val marker = Marker(binding.map)
                    marker.title = it.place.id
                    marker.snippet = it.place.type
                    marker.position = GeoPoint(it.place.lat, it.place.lon)
                    marker.icon = it.marker

                    marker.setOnMarkerClickListener { _, _ ->
                        model.selectPlace(it.place.id, false)
                        true
                    }

                    placesOverlay!!.add(marker)
                }

                binding.map.overlays.add(placesOverlay)
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
            val startPoint = GeoPoint(it.lat, it.lon)
            mapController.setCenter(startPoint)
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        lifecycleScope.launchWhenResumed {
            val snack = Snackbar.make(
                binding.root,
                "",
                Snackbar.LENGTH_INDEFINITE,
            )

            model.syncMessage.collectLatest {
                snack.setText(it)

                if (it.isNotBlank()) {
                    snack.show()
                } else {
                    snack.dismiss()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
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
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            requireActivity().finish()
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
                model.selectPlace("", false)
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

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.fab.isVisible = slideOffset < 0.5f
                (childFragmentManager.findFragmentById(R.id.placeDetailsFragment) as PlaceDetailsFragment).setScrollProgress(
                    slideOffset
                )
            }
        })
    }

    private suspend fun getPlaceDetailsToolbar(): Toolbar {
        while (placeDetailsFragment.view == null
            || placeDetailsFragment.view!!.findViewById<View>(R.id.toolbar)!!.height == 0
        ) {
            delay(10)
        }

        return placeDetailsFragment.view?.findViewById(R.id.toolbar)!!
    }
}