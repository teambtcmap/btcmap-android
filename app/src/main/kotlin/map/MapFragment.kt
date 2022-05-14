package map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.MenuItem
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
import org.btcmap.R
import org.btcmap.databinding.FragmentMapBinding
import search.PlacesSearchResultViewModel
import db.Place
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    companion object {
        private const val REQUEST_ACCESS_LOCATION = 10
        private const val DEFAULT_MAP_ZOOM = 15f
    }

    private val model: MapModel by viewModel()

    private val placesSearchResultModel: PlacesSearchResultViewModel by sharedViewModel()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    var locationOverlay: MyLocationNewOverlay? = null
    var placesOverlay: Overlay? = null

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                requireActivity().finish()
            }
        }
    }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(backPressedCallback)
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
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        binding.map.setMultiTouchControls(true)

        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), binding.map)
        locationOverlay.enableMyLocation()
        binding.map.overlays += locationOverlay

        binding.map.overlays += MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                viewLifecycleOwner.lifecycleScope.launch { model.selectPlace(0, false) }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })

        binding.map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                model.setMapViewport(binding.map.boundingBox)
                return false
            }

            override fun onZoom(event: ZoomEvent?) = false
        })

        bottomSheetBehavior = BottomSheetBehavior.from(binding.placeDetails).apply {
            state = BottomSheetBehavior.STATE_HIDDEN

            addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {

                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    binding.locationFab.isVisible = slideOffset < 0.5f
                    (childFragmentManager.findFragmentById(R.id.placeDetailsFragment) as PlaceDetailsFragment).setScrollProgress(slideOffset)
                }
            })

            viewLifecycleOwner.lifecycleScope.launch {
                while (true) {
                    val tb = (childFragmentManager.findFragmentById(R.id.placeDetailsFragment) as PlaceDetailsFragment).view?.findViewById<Toolbar>(R.id.toolbar)

                    if (tb != null && tb.height > 0) {
                        peekHeight = tb.height

                        tb.setOnClickListener {
                            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            } else {
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
                            }
                        }

                        break
                    } else {
                        delay(100)
                    }
                }
            }
        }

        binding.toolbar.apply {
            inflateMenu(R.menu.map)
            setOnMenuItemClickListener(this@MapFragment)
        }

        binding.placeDetails.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        model.selectedPlace.onEach {
            if (it != null) {
                val placeDetailsFragment = childFragmentManager.findFragmentById(R.id.placeDetailsFragment) as PlaceDetailsFragment

                while (placeDetailsFragment.view == null) {
                    delay(10)
                }

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

            val location = model.location.value
            val mapController = binding.map.controller
            mapController.setZoom(DEFAULT_MAP_ZOOM.toDouble())
            val startPoint = GeoPoint(location.latitude, location.longitude)
            mapController.setCenter(startPoint)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            placesSearchResultModel.place.collectLatest {
                if (it == null) {
                    return@collectLatest
                }

                val mapController = binding.map.controller
                mapController.setZoom(DEFAULT_MAP_ZOOM.toDouble())
                val startPoint = GeoPoint(it.lat, it.lon)
                mapController.setCenter(startPoint)
                model.selectPlace(it.id, true)

                placesSearchResultModel.consume()
            }
        }

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
                            viewLifecycleOwner.lifecycleScope.launch { model.selectPlace(itemsToPlaces[item]!!.id, false) }
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
            val startPoint = GeoPoint(it.latitude, it.longitude)
            mapController.setCenter(startPoint)
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        backPressedCallback.isEnabled = true
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        prevZoom = binding.map.zoomLevel
        _binding = null
        backPressedCallback.isEnabled = false
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

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                lifecycleScope.launch {
                    val action = MapFragmentDirections.actionMapFragmentToPlacesSearchFragment(
                        model.location.value.latitude.toString(),
                        model.location.value.longitude.toString(),
                    )

                    findNavController().navigate(action)
                }
            }

            R.id.action_add -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://wiki.openstreetmap.org/wiki/How_to_contribute")
                startActivity(intent)
            }

            R.id.action_settings -> {
                findNavController().navigate(R.id.action_mapFragment_to_settingsFragment)
            }

            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun requestLocationPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            REQUEST_ACCESS_LOCATION,
        )
    }
}