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
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle

import com.bubelov.coins.R
import com.bubelov.coins.model.Location
import com.bubelov.coins.util.toLatLng
import com.bubelov.coins.util.toLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import dagger.android.support.DaggerAppCompatActivity

import kotlinx.android.synthetic.main.activity_pick_location.*

class PickLocationActivity : DaggerAppCompatActivity() {
    private val map = MutableLiveData<GoogleMap>()

    private val initialLocation: Location
        get() = intent.getSerializableExtra(LOCATION_EXTRA) as Location

    private val initialZoom: Float
        get() = intent.getFloatExtra(ZOOM_EXTRA, 0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_location)

        toolbar.apply {
            setNavigationOnClickListener { finish() }
            inflateMenu(R.menu.pick_location)

            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_done) {
                    onDonePressed()
                    return@setOnMenuItemClickListener true
                }

                false
            }
        }

        (fragmentManager.findFragmentById(R.id.map) as MapFragment).getMapAsync({ map.value = it })

        map.observe(this, Observer { map ->
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation.toLatLng(), initialZoom))
        })
    }

    private fun onDonePressed() {
        val data = Intent().apply { putExtra(LOCATION_EXTRA, map.value?.cameraPosition?.target?.toLocation()) }
        setResult(Activity.RESULT_OK, data)
        supportFinishAfterTransition()
    }

    companion object {
        const val LOCATION_EXTRA = "location"
        const val ZOOM_EXTRA = "zoom"

        fun newIntent(
            context: Context,
            initialLocation: Location,
            initialZoom: Float
        ): Intent {
            return Intent(context, PickLocationActivity::class.java).apply {
                putExtra(LOCATION_EXTRA, initialLocation)
                putExtra(ZOOM_EXTRA, initialZoom)
            }
        }
    }
}