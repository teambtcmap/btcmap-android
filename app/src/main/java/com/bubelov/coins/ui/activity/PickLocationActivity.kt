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