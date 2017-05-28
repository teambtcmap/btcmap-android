package com.bubelov.coins.ui.activity

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.SeekBar

import com.bubelov.coins.Constants
import com.bubelov.coins.R
import com.bubelov.coins.model.NotificationArea
import com.bubelov.coins.ui.viewmodel.NotificationAreaViewModel
import com.bubelov.coins.util.OnSeekBarChangeAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

import kotlinx.android.synthetic.main.activity_notification_area.*

/**
 * @author Igor Bubelov
 */

class NotificationAreaActivity : AbstractActivity(), OnMapReadyCallback {
    private var viewModel = lazy { ViewModelProviders.of(this).get(NotificationAreaViewModel::class.java) }

    private var map: GoogleMap? = null

    private var areaCircle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_area)

        toolbar.setNavigationOnClickListener {
            saveArea()
            supportFinishAfterTransition()
        }

        val mapFragment = fragmentManager.findFragmentById(R.id.map_fragment) as MapFragment
        mapFragment.getMapAsync(this)

        seek_bar_radius.progressDrawable.setColorFilter(ContextCompat.getColor(this, R.color.accent), PorterDuff.Mode.SRC_IN)
        seek_bar_radius.thumb.setColorFilter(ContextCompat.getColor(this, R.color.accent), PorterDuff.Mode.SRC_IN)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        bottom_panel.post { map!!.setPadding(0, 0, 0, bottom_panel.height) }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.uiSettings.isMyLocationButtonEnabled = false
        map!!.uiSettings.isZoomControlsEnabled = false
        map!!.uiSettings.isCompassEnabled = false
        map!!.setOnMarkerDragListener(OnMarkerDragListener())

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map!!.isMyLocationEnabled = true
        }

        val defaultCameraPosition = intent.getParcelableExtra<CameraPosition>(DEFAULT_CAMERA_POSITION_EXTRA)
        showArea(viewModel.value.notificationArea ?: viewModel.value.getDefaultNotificationArea(defaultCameraPosition))
    }

    private fun showArea(area: NotificationArea) {
        val markerDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_location)

        val marker = map!!.addMarker(MarkerOptions()
                .position(LatLng(area.latitude, area.longitude))
                .icon(markerDescriptor)
                .anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V)
                .draggable(true))

        val circleOptions = CircleOptions()
                .center(marker.position)
                .radius(area.radius)
                .fillColor(ContextCompat.getColor(this, R.color.notification_area))
                .strokeColor(ContextCompat.getColor(this, R.color.notification_area_border))
                .strokeWidth(4f)

        areaCircle = map!!.addCircle(circleOptions)

        seek_bar_radius.max = 500000
        seek_bar_radius.progress = areaCircle!!.radius.toInt()
        seek_bar_radius.setOnSeekBarChangeListener(SeekBarChangeListener())

        val areaCenter = LatLng(area.latitude, area.longitude)
        map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(areaCenter, (viewModel.value.getZoomLevel(areaCircle!!) - 1).toFloat()))
    }

    private fun saveArea() {
        val area = NotificationArea(
                areaCircle!!.center.latitude,
                areaCircle!!.center.longitude,
                areaCircle!!.radius
        )

        viewModel.value.notificationArea = area
    }

    private inner class OnMarkerDragListener : GoogleMap.OnMarkerDragListener {
        override fun onMarkerDragStart(marker: Marker) {
            areaCircle!!.fillColor = ContextCompat.getColor(this@NotificationAreaActivity, android.R.color.transparent)
        }

        override fun onMarkerDrag(marker: Marker) {
            areaCircle!!.center = marker.position
            map!!.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
        }

        override fun onMarkerDragEnd(marker: Marker) {
            areaCircle!!.fillColor = ContextCompat.getColor(this@NotificationAreaActivity, R.color.notification_area)
            saveArea()
        }
    }

    private inner class SeekBarChangeListener : OnSeekBarChangeAdapter() {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            areaCircle!!.radius = progress.toDouble()
        }
    }

    companion object {
        val DEFAULT_CAMERA_POSITION_EXTRA = "default_camera_position"

        fun newIntent(context: Context, defaultCameraPosition: CameraPosition): Intent {
            val intent = Intent(context, NotificationAreaActivity::class.java)
            intent.putExtra(DEFAULT_CAMERA_POSITION_EXTRA, defaultCameraPosition)
            return intent
        }
    }
}