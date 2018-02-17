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

package com.bubelov.coins.ui.activity

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem
import android.view.View

import com.bubelov.coins.Constants
import com.bubelov.coins.R
import com.bubelov.coins.model.Place
import com.bubelov.coins.ui.widget.PlaceDetailsView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.squareup.picasso.Picasso

import com.bubelov.coins.ui.model.PlaceMarker
import com.bubelov.coins.ui.viewmodel.MapViewModel
import com.bubelov.coins.util.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.jetbrains.anko.longToast
import javax.inject.Inject

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Toolbar.OnMenuItemClickListener, MapViewModel.Callback {
    @Inject lateinit var modelFactory: ViewModelProvider.Factory
    private lateinit var model: MapViewModel

    private lateinit var drawerHeader: View

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private var map = MutableLiveData<GoogleMap>()

    private val placesManager = MutableLiveData<ClusterManager<PlaceMarker>>()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    @Inject internal lateinit var analytics: Analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        model = ViewModelProviders.of(this, modelFactory)[MapViewModel::class.java]
        model.callback = this

        drawerHeader = navigation_view.getHeaderView(0)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheetBehavior = BottomSheetBehavior.from(place_details)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
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
                R.id.action_settings -> openSettingsScreen()
            }

            true
        }

        model.selectedCurrency.observe(this, Observer {
            toolbar.title = getString(R.string.s_map, it!!.currencyCodeToName())
            toolbar.menu.findItem(R.id.action_add).isVisible = it == "BTC"
            navigation_view.menu.findItem(R.id.action_exchange_rates).isVisible = it == "BTC"
        })

        drawerToggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.open, R.string.close)
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

        fab.setOnClickListener {
            if (model.userLocation.hasLocationPermission) {
                val map = map.value
                val location = model.userLocation.value

                if (map != null && location != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLng(location.toLatLng()))
                }
            } else {
                requestLocationPermissions()
            }
        }

        place_details.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                val selectedPlace = model.selectedPlace.value

                if (selectedPlace != null) {
                    analytics.logViewContent(selectedPlace.id.toString(), selectedPlace.name, "place")
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

        model.userLocation.observe(this, Observer { location ->
            if (location == null) {
                return@Observer
            }

            model.onNewLocation(location)
            val map = this.map.value

            if (map != null && model.moveToNextLocation) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location.toLatLng(), MAP_DEFAULT_ZOOM))
                model.moveToNextLocation = false
            }
        })

        handleIntent(intent)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (!model.userLocation.hasLocationPermission) {
                requestLocationPermissions()
            }
        }

        if (requestCode == REQUEST_FIND_PLACE && resultCode == Activity.RESULT_OK) {
            moveToPlace(data?.getLongExtra(PlacesSearchActivity.PLACE_ID_EXTRA, 0) ?: 0)
        }

        if (requestCode == REQUEST_ADD_PLACE && resultCode == Activity.RESULT_OK) {
            longToast(R.string.place_has_been_added)
        }

        if (requestCode == REQUEST_EDIT_PLACE && resultCode == Activity.RESULT_OK) {
            longToast(R.string.your_edits_have_been_submitted)
        }

        if (requestCode == REQUEST_SIGN_IN && resultCode == Activity.RESULT_OK) {
            updateDrawerHeader()
        }

        if (requestCode == REQUEST_PROFILE && resultCode == ProfileActivity.RESULT_SIGN_OUT) {
            updateDrawerHeader()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ACCESS_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                model.userLocation.onLocationPermissionGranted()
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_add -> model.onAddPlaceClick()
            R.id.action_search -> PlacesSearchActivity.startForResult(this, model.userLocation.value, model.selectedCurrency.value ?: SelectedCurrencyLiveData.DEFAULT_CURRENCY, REQUEST_FIND_PLACE)
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(navigation_view)) {
            drawer_layout.closeDrawer(navigation_view)
            return
        }

        when (bottomSheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED, BottomSheetBehavior.STATE_SETTLING -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN)
            BottomSheetBehavior.STATE_HIDDEN -> super.onBackPressed()
        }
    }

    override fun signIn() {
        startActivityForResult(SignInActivity.newIntent(this), REQUEST_SIGN_IN)
    }

    override fun addPlace() {
        EditPlaceActivity.startForResult(this, null, map.value!!.cameraPosition, REQUEST_ADD_PLACE)
    }

    override fun editPlace(place: Place) {
        EditPlaceActivity.startForResult(this, place, map.value!!.cameraPosition, REQUEST_EDIT_PLACE)
    }

    override fun showUserProfile() {
        startActivityForResult(ProfileActivity.newIntent(this), REQUEST_PROFILE)
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MapActivity.REQUEST_ACCESS_LOCATION)
    }

    override fun onMapReady(map: GoogleMap) {
        this.map.value = map

        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isMapToolbarEnabled = false

        initClustering(map)

        if (!model.userLocation.hasLocationPermission) {
            requestLocationPermissions()
        }
    }

    private fun updateDrawerHeader() {
        if (model.userRepository.signedIn()) {
            val user = model.userRepository.user!!

            if (!TextUtils.isEmpty(user.avatarUrl)) {
                Picasso.with(this).load(user.avatarUrl).into(drawerHeader.avatar)
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

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(PLACE_ID_EXTRA)) {
            moveToPlace(intent.getLongExtra(PLACE_ID_EXTRA, 0))
        }

        updateDrawerHeader()
    }

    private fun moveToPlace(id: Long) {
        model.selectedPlaceId.value = id

        model.selectedPlace.observe(this, object: Observer<Place?> {
            override fun onChanged(place: Place?) {
                if (place != null) {
                    map.observe(this@MapActivity, Observer { map ->
                        model.moveToNextLocation = false
                        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(place.toLatLng(), MAP_DEFAULT_ZOOM))
                    })
                }

                model.selectedPlace.removeObserver(this)
            }
        })
    }

    private fun openExchangeRatesScreen() {
        val intent = Intent(this@MapActivity, ExchangeRatesActivity::class.java)
        startActivity(intent)
    }

    private fun openNotificationAreaScreen() {
        val intent = NotificationAreaActivity.newIntent(this, map.value!!.cameraPosition)
        startActivity(intent)
    }

    private fun openSupportChat() {
        openUrl("https://t.me/joinchat/AAAAAAwVT4aVBdFzcKKbsw")
        analytics.logViewContent("chat", null, "screen")
    }

    private fun openSettingsScreen() {
        val intent = SettingsActivity.newIntent(this)
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle())
    }

    private fun initClustering(map: GoogleMap) {
        val placesManager = ClusterManager<PlaceMarker>(this, map)
        placesManager.setAnimation(false)
        val renderer = PlacesRenderer(this, map, placesManager)
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

    private inner class PlacesRenderer internal constructor(context: Context, map: GoogleMap, clusterManager: ClusterManager<PlaceMarker>) : DefaultClusterRenderer<PlaceMarker>(context, map, clusterManager) {
        override fun onBeforeClusterItemRendered(placeMarker: PlaceMarker, markerOptions: MarkerOptions) {
            super.onBeforeClusterItemRendered(placeMarker, markerOptions)

            markerOptions
                    .icon(BitmapDescriptorFactory.fromBitmap(placeMarker.icon))
                    .anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V)
        }
    }

    private inner class ClusterItemClickListener : ClusterManager.OnClusterItemClickListener<PlaceMarker> {
        override fun onClusterItemClick(placeMarker: PlaceMarker): Boolean {
            model.selectedPlaceId.value = placeMarker.placeId
            return true
        }
    }

    companion object {
        private val PLACE_ID_EXTRA = "place_id"

        private val REQUEST_CHECK_LOCATION_SETTINGS = 10
        private val REQUEST_ACCESS_LOCATION = 20
        private val REQUEST_FIND_PLACE = 30
        private val REQUEST_ADD_PLACE = 40
        private val REQUEST_EDIT_PLACE = 50
        private val REQUEST_SIGN_IN = 60
        private val REQUEST_PROFILE = 70

        private val MAP_DEFAULT_ZOOM = 13f

        fun newIntent(context: Context, placeId: Long): Intent {
            val intent = Intent(context, MapActivity::class.java)

            if (placeId != 0L) {
                intent.putExtra(PLACE_ID_EXTRA, placeId)
            }

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            return intent
        }
    }
}