package map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import area.AreaResultModel
import area.AreasFragment
import area.polygons
import com.google.android.material.bottomsheet.BottomSheetBehavior
import delivery.DeliveryFragment
import donation.DonationFragment
import element.ElementFragment
import element.ElementsCluster
import event.EventsFragment
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
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.OnCameraIdleListener
import reports.ReportsFragment
import search.SearchAdapter
import search.SearchModel
import search.SearchResultModel
import settings.SettingsFragment
import user.UsersFragment
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

    private var emptyClusterBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.searchBar) { view, windowInsets ->
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.fab.translationY = -navBarsInsets.bottom.toFloat()
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

        binding.searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_donate -> {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<DonationFragment>(R.id.nav_host_fragment)
                        addToBackStack(null)
                    }
                }

                R.id.action_add_element -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://btcmap.org/add-location")
                    startActivity(intent)
                }

                R.id.action_delivery -> {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<DeliveryFragment>(
                            R.id.nav_host_fragment, null, bundleOf(
                                "userLat" to model.mapViewport.value.boundingBox.center.latitude.toFloat(),
                                "userLon" to model.mapViewport.value.boundingBox.center.longitude.toFloat(),
                                "searchAreaId" to 662L,
                            )
                        )
                        addToBackStack(null)
                    }
                }

                R.id.action_areas -> {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<AreasFragment>(
                            R.id.nav_host_fragment, null, bundleOf(
                                "lat" to model.mapViewport.value.boundingBox.center.latitude.toFloat(),
                                "lon" to model.mapViewport.value.boundingBox.center.longitude.toFloat(),
                            )
                        )
                        addToBackStack(null)
                    }
                }

                R.id.action_trends -> {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<ReportsFragment>(R.id.nav_host_fragment)
                        addToBackStack(null)
                    }
                }

                R.id.action_users -> {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<UsersFragment>(R.id.nav_host_fragment)
                        addToBackStack(null)
                    }
                }

                R.id.action_events -> {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<EventsFragment>(R.id.nav_host_fragment)
                        addToBackStack(null)
                    }
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
            it.initStyle(requireContext())
            it.uiSettings.isCompassEnabled = false
            it.uiSettings.isRotateGesturesEnabled = false
            it.addCancelSelectionOverlay()
            it.setOnMarkerClickListener { marker ->
                if (!marker.snippet.isNullOrBlank()) {
                    val id = marker.snippet.toLong()
                    model.selectElement(id, false)
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
                binding.map.getMapAsync {
                    it.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                userLocation.latitude, userLocation.longitude
                            ),
                            14.0,
                        )
                    )
                }
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
            model.visibleElements.collectLatest { newElements ->
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
                                        markersRepo.getMarker(
                                            it.cluster.iconId.ifBlank { "question_mark" },
                                            it.cluster.comments,
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

                            is MapModel.MapItem.Meetup -> {}
                        }
                    }
                }
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
                val pickedPlace = searchResultModel.element.value
                searchResultModel.element.update { null }

                val pickedArea = areaResultModel.area.value
                areaResultModel.area.update { null }

                if (pickedPlace != null) {
                    binding.map.getMapAsync {
                        model.selectElement(pickedPlace.id, true)
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
                        val boundingBox = boundingBox(pickedArea.tags.polygons())
                        val boundingBoxPaddingPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            16f,
                            resources.displayMetrics,
                        ).toInt()
                        it.moveCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                boundingBox,
                                boundingBoxPaddingPx
                            )
                        )
                    }
                    return@repeatOnLifecycle
                }

                val firstBoundingBox =
                    model.mapViewport.firstOrNull()?.boundingBox!!

                binding.map.getMapAsync {
                    it.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            firstBoundingBox, 0
                        )
                    )
                }
            }
        }

        val searchAdapter = SearchAdapter { row ->
            binding.searchView.clearText()
            binding.searchView.hide()

            model.selectElement(row.element.id, true)
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
            model.selectElement(0, false)
            true
        }
    }

    private val onCameraIdleListener = OnCameraIdleListener {
        binding.map.getMapAsync { map ->
            model.setMapViewport(
                MapModel.MapViewport(
                    zoom = map.cameraPosition.zoom,
                    boundingBox = map.projection.visibleRegion.latLngBounds,
                )
            )
            searchModel.setLocation(
                LatLng(
                    map.cameraPosition.target!!.latitude, map.cameraPosition.target!!.longitude
                )
            )
        }
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
}