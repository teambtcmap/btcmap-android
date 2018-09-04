/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.bubelov.coins.R
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bubelov.coins.BuildConfig
import com.bubelov.coins.model.Place
import com.bubelov.coins.ui.activity.EditPlaceActivity
import com.bubelov.coins.ui.activity.ExchangeRatesActivity
import com.bubelov.coins.ui.activity.NotificationAreaActivity
import com.bubelov.coins.ui.activity.PlacesSearchActivity
import com.bubelov.coins.ui.activity.ProfileActivity
import com.bubelov.coins.ui.activity.SignInActivity
import com.bubelov.coins.ui.model.PlaceMarker
import com.bubelov.coins.ui.viewmodel.MapViewModel
import com.bubelov.coins.ui.widget.PlaceDetailsView
import com.bubelov.coins.util.Analytics
import com.bubelov.coins.util.currencyCodeToName
import com.bubelov.coins.util.emptyPlace
import com.bubelov.coins.util.openUrl
import com.bubelov.coins.util.toLatLng
import com.bubelov.coins.util.viewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.squareup.picasso.Picasso
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import javax.inject.Inject

class MapFragment :
    DaggerFragment(),
    OnMapReadyCallback,
    Toolbar.OnMenuItemClickListener,
    MapViewModel.Callback {
    @Inject internal lateinit var modelFactory: ViewModelProvider.Factory
    private val model by lazy { viewModelProvider(modelFactory) as MapViewModel }

    private lateinit var drawerHeader: View

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private var map = MutableLiveData<GoogleMap>()

    private val placesManager = MutableLiveData<ClusterManager<PlaceMarker>>()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    @Inject internal lateinit var analytics: Analytics

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.callback = this

        drawerHeader = navigation_view.getHeaderView(0)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheetBehavior = BottomSheetBehavior.from(place_details)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                place_details.fullScreen = newState == BottomSheetBehavior.STATE_EXPANDED
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                fab.visibility = if (slideOffset > 0.5f) View.GONE else View.VISIBLE
            }
        })

        bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(R.dimen.map_header_height)

        place_details.callback = object : PlaceDetailsView.Callback {
            override fun onEditPlaceClick(place: Place) {
                model.onEditPlaceClick(place)
            }

            override fun onDismissed() {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            override fun onShared(place: Place) {
                analytics.logShareContent(place.id.toString(), place.name, "place")
            }
        }

        toolbar.setNavigationOnClickListener { drawer_layout.openDrawer(navigation_view) }
        toolbar.inflateMenu(R.menu.map)
        toolbar.setOnMenuItemClickListener(this)

        navigation_view.setNavigationItemSelectedListener { item ->
            drawer_layout.closeDrawers()

            when (item.itemId) {
                R.id.action_exchange_rates -> openExchangeRatesScreen()
                R.id.action_notification_area -> openNotificationAreaScreen()
                R.id.action_chat -> openSupportChat()
            }

            true
        }

        model.selectedCurrency.observe(this, Observer {
            toolbar.title = getString(R.string.s_map, it!!.currencyCodeToName())
            toolbar.menu.findItem(R.id.action_add).isVisible = it == "BTC"
            navigation_view.menu.findItem(R.id.action_exchange_rates).isVisible = it == "BTC"
        })

        drawerToggle = ActionBarDrawerToggle(
            requireActivity(),
            drawer_layout,
            toolbar,
            R.string.open,
            R.string.close
        )

        drawer_layout.addDrawerListener(drawerToggle)

        updateDrawerHeader()

        placesManager.observe(this, Observer { placesManager ->
            if (placesManager != null) {
                model.placeMarkers.observe(this, Observer { markers ->
                    placesManager.clearItems()
                    placesManager.addItems(markers)
                    placesManager.cluster()
                })
            }
        })

        place_details.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                val selectedPlace = model.selectedPlace.value

                if (selectedPlace != null) {
                    analytics.logViewContent(
                        selectedPlace.id.toString(),
                        selectedPlace.name,
                        "place"
                    )
                }
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        model.selectedPlace.observe(this, Observer {
            if (it != null) {
                place_details.setPlace(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                analytics.logSelectContent(it.id.toString(), it.name, "place")
            }
        })

        model.userLocation.observe(this, Observer {
            fab.setOnClickListener {
                if (!model.isLocationPermissionGranted()) {
                    requestLocationPermissions()
                    return@setOnClickListener
                }

                moveToUserLocation()
            }
        })

        navigation_view.setupWithNavController(findNavController())

        Handler().post {
            drawerToggle.syncState()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (!model.isLocationPermissionGranted()) {
                requestLocationPermissions()
            }
        }

        if (requestCode == REQUEST_FIND_PLACE && resultCode == Activity.RESULT_OK) {
            model.navigateToNextSelectedPlace = true
            model.selectedPlaceId.value = data?.getLongExtra(
                PlacesSearchActivity.PLACE_ID_EXTRA,
                0
            ) ?: 0
        }

        if (requestCode == REQUEST_ADD_PLACE && resultCode == Activity.RESULT_OK) {
            Toast.makeText(requireContext(), R.string.place_has_been_added, Toast.LENGTH_LONG)
                .show()
        }

        if (requestCode == REQUEST_EDIT_PLACE && resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                requireContext(),
                R.string.your_edits_have_been_submitted,
                Toast.LENGTH_LONG
            ).show()
        }

        if (requestCode == REQUEST_SIGN_IN && resultCode == Activity.RESULT_OK) {
            updateDrawerHeader()
        }

        if (requestCode == REQUEST_PROFILE && resultCode == ProfileActivity.RESULT_SIGN_OUT) {
            updateDrawerHeader()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_ACCESS_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                model.onLocationPermissionGranted()
                map.value?.isMyLocationEnabled = true
                moveToUserLocation()
            }
        }
    }

    private fun moveToUserLocation() {
        val location = model.userLocation.value

        if (location != null) {
            map.value?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    location.toLatLng(),
                    DEFAULT_MAP_ZOOM
                )
            )
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_add -> model.onAddPlaceClick()

            R.id.action_search -> PlacesSearchActivity.startForResult(
                requireActivity(),
                model.userLocation.value,
                model.selectedCurrency.value ?: "",
                REQUEST_FIND_PLACE
            )

            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun signIn() {
        startActivityForResult(SignInActivity.newIntent(requireContext()), REQUEST_SIGN_IN)
    }

    override fun addPlace() {
        val place = emptyPlace().copy(
            latitude = map.value?.cameraPosition?.target?.latitude ?: 0.0,
            longitude = map.value?.cameraPosition?.target?.longitude ?: 0.0
        )

        EditPlaceActivity.startForResult(requireActivity(), place, REQUEST_ADD_PLACE)
    }

    override fun editPlace(place: Place) {
        EditPlaceActivity.startForResult(requireActivity(), place, REQUEST_EDIT_PLACE)
    }

    override fun showUserProfile() {
        startActivityForResult(ProfileActivity.newIntent(requireContext()), REQUEST_PROFILE)
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_ACCESS_LOCATION
        )
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        this.map.value = map

        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isMapToolbarEnabled = false

        initClustering(map)

        if (model.isLocationPermissionGranted()) {
            map.isMyLocationEnabled = true

            if (model.selectedPlaceId.value == null || model.selectedPlaceId.value == 0L) {
                moveToUserLocation()
            }
        } else {
            requestLocationPermissions()
        }

        model.selectedPlace.observe(this, Observer { place ->
            if (place != null && model.navigateToNextSelectedPlace) {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        place.toLatLng(),
                        DEFAULT_MAP_ZOOM
                    )
                )

                model.navigateToNextSelectedPlace = false
            }
        })
    }

    private fun updateDrawerHeader() {
        val user = model.userRepository.user

        if (user != null) {
            if (!TextUtils.isEmpty(user.avatarUrl)) {
                Picasso.with(requireContext()).load(user.avatarUrl).into(drawerHeader.avatar)
            } else {
                drawerHeader.avatar.setImageResource(R.drawable.ic_no_avatar)
            }

            if (!TextUtils.isEmpty(user.firstName)) {
                drawerHeader.user_name.text = String.format("%s %s", user.firstName, user.lastName)
            } else {
                drawerHeader.user_name.text = user.email
            }
        } else {
            drawerHeader.avatar.setImageResource(R.drawable.ic_no_avatar)
            drawerHeader.user_name.setText(R.string.guest)
        }

        drawerHeader.setOnClickListener {
            model.onDrawerHeaderClick()
            drawer_layout.closeDrawers()
        }
    }

    private fun openExchangeRatesScreen() {
        val intent = Intent(requireContext(), ExchangeRatesActivity::class.java)
        startActivity(intent)
    }

    private fun openNotificationAreaScreen() {
        val intent =
            NotificationAreaActivity.newIntent(requireContext(), map.value!!.cameraPosition)
        startActivity(intent)
    }

    private fun openSupportChat() {
        requireContext().openUrl("https://t.me/joinchat/AAAAAAwVT4aVBdFzcKKbsw")
        analytics.logViewContent("chat", null, "screen")
    }

    private fun initClustering(map: GoogleMap) {
        val placesManager = ClusterManager<PlaceMarker>(requireContext(), map)
        placesManager.setAnimation(false)
        val renderer = PlacesRenderer(requireContext(), map, placesManager)
        renderer.setAnimation(false)
        placesManager.renderer = renderer
        renderer.setOnClusterItemClickListener(ClusterItemClickListener())

        map.setOnCameraIdleListener {
            placesManager.onCameraIdle()
            model.mapBounds.value = map.projection.visibleRegion.latLngBounds
        }

        map.setOnMarkerClickListener(placesManager)

        map.setOnMapClickListener {
            model.selectedPlaceId.value = 0
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN)
        }

        this.placesManager.value = placesManager
    }

    private inner class PlacesRenderer internal constructor(
        context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<PlaceMarker>
    ) : DefaultClusterRenderer<PlaceMarker>(context, map, clusterManager) {
        override fun onBeforeClusterItemRendered(
            placeMarker: PlaceMarker,
            markerOptions: MarkerOptions
        ) {
            super.onBeforeClusterItemRendered(placeMarker, markerOptions)

            markerOptions
                .icon(BitmapDescriptorFactory.fromBitmap(placeMarker.icon))
                .anchor(BuildConfig.MAP_MARKER_ANCHOR_U, BuildConfig.MAP_MARKER_ANCHOR_V)
        }
    }

    private inner class ClusterItemClickListener :
        ClusterManager.OnClusterItemClickListener<PlaceMarker> {
        override fun onClusterItemClick(placeMarker: PlaceMarker): Boolean {
            model.selectedPlaceId.value = placeMarker.placeId
            return true
        }
    }

    companion object {
        private const val REQUEST_CHECK_LOCATION_SETTINGS = 10
        private const val REQUEST_ACCESS_LOCATION = 20
        private const val REQUEST_FIND_PLACE = 30
        private const val REQUEST_ADD_PLACE = 40
        private const val REQUEST_EDIT_PLACE = 50
        private const val REQUEST_SIGN_IN = 60
        private const val REQUEST_PROFILE = 70

        private const val DEFAULT_MAP_ZOOM = 15f
    }
}