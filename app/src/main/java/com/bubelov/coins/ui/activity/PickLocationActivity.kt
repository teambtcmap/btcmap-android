package com.bubelov.coins.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.bubelov.coins.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

import kotlinx.android.synthetic.main.activity_pick_location.*

/**
 * @author Igor Bubelov
 */

class PickLocationActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_location)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
        toolbar.inflateMenu(R.menu.pick_location)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_done -> {
                    val data = Intent().apply { putExtra(LOCATION_EXTRA, map.cameraPosition.target) }
                    setResult(Activity.RESULT_OK, data)
                    supportFinishAfterTransition()
                    true
                } else -> false
            }
        }

        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val initialLocation = intent.getParcelableExtra<LatLng>(LOCATION_EXTRA)

        if (initialLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, DEFAULT_ZOOM))
        } else {
            val cameraPosition = intent.getParcelableExtra<CameraPosition>(MAP_CAMERA_POSITION_EXTRA)

            if (cameraPosition != null) {
                map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    companion object {
        val LOCATION_EXTRA = "location"

        val MAP_CAMERA_POSITION_EXTRA = "map_camera_position"

        private val DEFAULT_ZOOM = 13f

        fun newIntent(context: Context, initialLocation: LatLng?, mapCameraPosition: CameraPosition)
                = Intent(context, PickLocationActivity::class.java).apply {
                    putExtra(LOCATION_EXTRA, initialLocation)
                    putExtra(MAP_CAMERA_POSITION_EXTRA, mapCameraPosition)
        }
    }
}