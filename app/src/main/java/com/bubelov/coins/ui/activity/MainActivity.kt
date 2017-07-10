package com.bubelov.coins.ui.activity

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem
import android.view.View

import com.bubelov.coins.Constants
import com.bubelov.coins.R
import com.bubelov.coins.model.Place
import com.bubelov.coins.ui.widget.PlaceDetailsView
import com.bubelov.coins.util.Analytics
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.squareup.picasso.Picasso

import com.bubelov.coins.ui.model.PlaceMarker
import com.bubelov.coins.ui.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*

/**
 * @author Igor Bubelov
 */

class MainActivity : AbstractActivity(), OnMapReadyCallback, Toolbar.OnMenuItemClickListener, MainViewModel.Callback {
    lateinit var drawerHeader: View

    lateinit var drawerToggle: ActionBarDrawerToggle

    lateinit var viewModel: MainViewModel

    var map: GoogleMap? = null

    lateinit var placesManager: ClusterManager<PlaceMarker>

    lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.callback = this

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

        Handler(Looper.getMainLooper()).postDelayed({ bottomSheetBehavior.setPeekHeight(place_details.headerHeight) }, 1000)

        place_details.callback = object : PlaceDetailsView.Callback {
            override fun onEditPlaceClick(place: Place) {
                viewModel.onEditPlaceClick(place)
            }

            override fun onDismissed() {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
                R.id.action_chat -> viewModel.onSupportChatClick()
                R.id.action_settings -> openSettingsScreen()
            }

            true
        }

        drawerToggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.open, R.string.close)
        drawer_layout.addDrawerListener(drawerToggle)

        updateDrawerHeader()

        viewModel.placeMarkers.observe(this, Observer { markers ->
            placesManager.clearItems()
            placesManager.addItems(markers)
            placesManager.cluster()
        })

        fab.setOnClickListener { viewModel.locateUser() }

        place_details.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                viewModel.onSelectedPlaceDetailsClick()
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        viewModel.selectedPlace.observe(this, Observer { place ->
            if (place != null) {
                place_details.setPlace(place)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })
    }

    //

    override fun signIn() {
        startActivityForResult(SignInActivity.newIntent(this), REQUEST_SIGN_IN)
    }

    override fun addPlace() {
        EditPlaceActivity.startForResult(this, 0, map!!.cameraPosition, REQUEST_ADD_PLACE)
    }

    override fun editPlace(place: Place) {
        EditPlaceActivity.startForResult(this, place.id, map!!.cameraPosition, REQUEST_EDIT_PLACE)
    }

    override fun startSearch(location: Location?) {
        PlacesSearchActivity.startForResult(this, location, REQUEST_FIND_PLACE)
    }

    override fun showUserProfile() {
        startActivityForResult(ProfileActivity.newIntent(this), REQUEST_PROFILE)
    }

    override fun selectPlace(place: Place) {
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(place.latitude, place.longitude), MAP_DEFAULT_ZOOM))
        place_details.setPlace(place)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MainActivity.REQUEST_ACCESS_LOCATION)
    }

    override fun moveToLocation(location: LatLng) {
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, MAP_DEFAULT_ZOOM))
    }

    //

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
            viewModel.onLocationPermissionGranted()
            viewModel.locateUser()
        }

        if (requestCode == REQUEST_FIND_PLACE && resultCode == Activity.RESULT_OK) {
            viewModel.selectPlace(data!!.getLongExtra(PlacesSearchActivity.PLACE_ID_EXTRA, -1))
        }

        if (requestCode == REQUEST_ADD_PLACE && resultCode == Activity.RESULT_OK) {
            TODO()
        }

        if (requestCode == REQUEST_EDIT_PLACE && resultCode == Activity.RESULT_OK) {
            TODO()
        }

        if (requestCode == REQUEST_SIGN_IN && resultCode == Activity.RESULT_OK) {
            updateDrawerHeader()
        }

        if (requestCode == REQUEST_PROFILE && resultCode == ProfileActivity.RESULT_SIGN_OUT) {
            updateDrawerHeader()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_add -> viewModel.onAddPlaceClick()
            R.id.action_search -> viewModel.onSearchClick()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ACCESS_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.onLocationPermissionGranted()
                viewModel.locateUser()
            }
        }
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.uiSettings.isMyLocationButtonEnabled = false
        map!!.uiSettings.isZoomControlsEnabled = false
        map!!.uiSettings.isCompassEnabled = false
        map!!.uiSettings.isMapToolbarEnabled = false

        initClustering()
        viewModel.locateUser()
        handleIntent(intent)
    }

    private fun updateDrawerHeader() {
        if (viewModel.userRepository.signedIn()) {
            val user = viewModel.userRepository.user!!

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
            viewModel.onDrawerHeaderClick()
            drawer_layout.closeDrawers()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra(CLEAR_PLACE_NOTIFICATIONS_EXTRA, false)) {
            viewModel.clearPlaceNotifications()
        }

        if (intent.hasExtra(PLACE_ID_EXTRA)) {
            viewModel.selectPlace(intent.getLongExtra(PLACE_ID_EXTRA, -1))
        }
    }

    private fun openExchangeRatesScreen() {
        val intent = Intent(this@MainActivity, ExchangeRatesActivity::class.java)
        startActivity(intent)
        Analytics.logSelectContent("exchange_rates", null, "screen")
    }

    private fun openNotificationAreaScreen() {
        val intent = NotificationAreaActivity.newIntent(this, map!!.cameraPosition)
        startActivity(intent)
        Analytics.logSelectContent("notification_area", null, "screen")
    }

    private fun openSettingsScreen() {
        val intent = SettingsActivity.newIntent(this)
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle())
    }

    private fun initClustering() {
        placesManager = ClusterManager<PlaceMarker>(this, map)
        placesManager.setAnimation(false)
        val renderer = PlacesRenderer(this, map!!, placesManager)
        renderer.setAnimation(false)
        placesManager.renderer = renderer
        renderer.setOnClusterItemClickListener(ClusterItemClickListener())

        map!!.setOnCameraIdleListener {
            placesManager.onCameraIdle()
            viewModel.mapBounds = map!!.projection.visibleRegion.latLngBounds
        }

        map!!.setOnMarkerClickListener(placesManager)

        map!!.setOnMapClickListener {
            viewModel.clearSelection()
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    private inner class PlacesRenderer internal constructor(context: Context, map: GoogleMap, clusterManager: ClusterManager<PlaceMarker>) : DefaultClusterRenderer<PlaceMarker>(context, map, clusterManager) {
        override fun onBeforeClusterItemRendered(placeMarker: PlaceMarker, markerOptions: MarkerOptions) {
            super.onBeforeClusterItemRendered(placeMarker, markerOptions)

            markerOptions
                    .icon(placeMarker.icon)
                    .anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V)
        }
    }

    private inner class ClusterItemClickListener : ClusterManager.OnClusterItemClickListener<PlaceMarker> {
        override fun onClusterItemClick(placeMarker: PlaceMarker): Boolean {
            viewModel.selectPlace(placeMarker.placeId)
            return false
        }
    }

    companion object {
        private val PLACE_ID_EXTRA = "place_id"
        private val CLEAR_PLACE_NOTIFICATIONS_EXTRA = "clear_place_notifications"

        private val REQUEST_CHECK_LOCATION_SETTINGS = 10
        private val REQUEST_ACCESS_LOCATION = 20
        private val REQUEST_FIND_PLACE = 30
        private val REQUEST_ADD_PLACE = 40
        private val REQUEST_EDIT_PLACE = 50
        private val REQUEST_SIGN_IN = 60
        private val REQUEST_PROFILE = 70

        private val MAP_DEFAULT_ZOOM = 13f

        fun newIntent(context: Context, placeId: Long): Intent {
            val intent = Intent(context, MainActivity::class.java)

            if (placeId != 0L) {
                intent.putExtra(PLACE_ID_EXTRA, placeId)
            }

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(CLEAR_PLACE_NOTIFICATIONS_EXTRA, true)
            return intent
        }
    }
}