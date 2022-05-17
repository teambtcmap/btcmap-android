package map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import org.btcmap.R
import org.btcmap.databinding.FragmentMapBinding
import search.PlacesSearchResultModel
import db.Place
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    companion object {
        private const val REQUEST_ACCESS_LOCATION = 10
        private const val DEFAULT_MAP_ZOOM = 15f
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

        binding.locationFab.setOnClickListener {
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

        var placesOverlay: Overlay? = null

        viewLifecycleOwner.lifecycleScope.launch {
            model.visiblePlaces.collectLatest { places ->
                if (placesOverlay != null) {
                    binding.map.overlays.remove(placesOverlay)
                }

                val items = mutableListOf<OverlayItem>()
                val itemsToPlaces = mutableMapOf<OverlayItem, Place>()

                places.forEach { place ->
                    val item = OverlayItem(
                        "Title",
                        "Description",
                        GeoPoint(place.first.lat, place.first.lon)
                    ).apply {
                        setMarker(place.second)
                    }

                    items += item
                    itemsToPlaces[item] = place.first
                }

                placesOverlay =
                    ItemizedIconOverlay(requireContext(), items, object : OnItemGestureListener<OverlayItem> {
                        override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                            model.selectPlace(itemsToPlaces[item]!!.id, false)
                            return true
                        }

                        override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                            return false
                        }
                    })

                binding.map.overlays.add(placesOverlay)
                binding.map.invalidate()
            }
        }

        var ignoreNextLocation = false

        if (model.viewport.value != null) {
            ignoreNextLocation = true

            val viewport = model.viewport.value!!
            val mapController = binding.map.controller
            mapController.setZoom(prevZoom)
            val viewportCenter = GeoPoint(viewport.centerLatitude, viewport.centerLongitude)
            mapController.setCenter(viewportCenter)
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

        model.syncPlaces()

        lifecycleScope.launchWhenResumed {
            val snack = Snackbar.make(
                binding.root,
                "",
                Snackbar.LENGTH_INDEFINITE,
            )

            model.toast.collectLatest {
                snack.setText(it)

                if (it.isNotBlank()) {
                    snack.show()
                } else {
                    snack.dismiss()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
        backPressedCallback.isEnabled = true
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
        backPressedCallback.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        prevZoom = binding.map.zoomLevel
        _binding = null
    }

    var prevZoom = 0

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            REQUEST_ACCESS_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    model.onLocationPermissionGranted()
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            REQUEST_ACCESS_LOCATION,
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
                model.selectPlace(0, false)
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
                binding.locationFab.isVisible = slideOffset < 0.5f
                (childFragmentManager.findFragmentById(org.btcmap.R.id.placeDetailsFragment) as PlaceDetailsFragment).setScrollProgress(
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