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
import android.arch.lifecycle.*
import android.arch.lifecycle.Observer
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

import com.bubelov.coins.R
import com.bubelov.coins.model.Place
import com.bubelov.coins.ui.viewmodel.EditPlaceViewModel
import com.bubelov.coins.util.toLatLng
import com.bubelov.coins.util.toLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_edit_place.*
import org.jetbrains.anko.alert
import javax.inject.Inject

class EditPlaceActivity : AppCompatActivity() {
    @Inject lateinit var modelFactory: ViewModelProvider.Factory
    private lateinit var model: EditPlaceViewModel

    private val map = MutableLiveData<GoogleMap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_place)

        model = ViewModelProviders.of(this, modelFactory)[EditPlaceViewModel::class.java]
        model.init(intent.getSerializableExtra(PLACE_EXTRA) as Place)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
        toolbar.inflateMenu(R.menu.edit_place)

        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_send) {
                submit()
            }

            true
        }

        val place = model.place

        if (place.id == 0L) {
            toolbar.setTitle(R.string.action_add_place)
            closed_switch.visibility = View.GONE
            change_location.setText(R.string.set_location)
        } else {
            name.setText(place.name)
            change_location.setText(R.string.change_location)
            phone.setText(place.phone)
            website.setText(place.website)
            description.setText(place.description)
            opening_hours.setText(place.openingHours)
        }

        (supportFragmentManager.findFragmentById(R.id.map) as MapFragment).getMapAsync({
            map.value = it
        })

        closed_switch.setOnCheckedChangeListener { _, checked ->
            name.isEnabled = !checked
            phone.isEnabled = !checked
            website.isEnabled = !checked
            description.isEnabled = !checked
            opening_hours.isEnabled = !checked
        }

        change_location.setOnClickListener {
            val intent = PickLocationActivity.newIntent(
                this,
                LatLng(place.latitude, place.longitude).toLocation(),
                map.value?.cameraPosition?.zoom ?: DEFAULT_ZOOM
            )

            startActivityForResult(intent, REQUEST_PICK_LOCATION)
        }

        model.showProgress.observe(this, Observer {
            state_switcher.displayedChild = if (it == true) 1 else 0
        })

        map.observe(this, Observer { map ->
            if (map == null) return@Observer

            map.uiSettings.setAllGesturesEnabled(false)

            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(place.latitude, place.longitude),
                    DEFAULT_ZOOM
                )
            )
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PICK_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            val location = data.getParcelableExtra(PickLocationActivity.LOCATION_EXTRA) as Location

            map.value?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    location.toLatLng(),
                    DEFAULT_ZOOM
                )
            )

            change_location.setText(R.string.change_location)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun submit() {
        syncUIWithModel()

        if (name.length() == 0) {
            alert(messageResource = R.string.name_is_not_specified).show()
            return
        }

        if (model.place.latitude == 0.0 && model.place.longitude == 0.0) {
            alert(messageResource = R.string.location_is_not_specified).show()
            return
        }

        model.submitChanges().observe(this, Observer { success ->
            if (success == true) {
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                alert(messageResource = R.string.could_not_submit_changes).show()
            }
        })
    }

    private fun syncUIWithModel() {
        model.place = model.place.copy(
            visible = !closed_switch.isChecked,
            name = name.text.toString(),
            description = description.text.toString(),
            latitude = map.value?.cameraPosition?.target?.latitude ?: 0.0,
            longitude = map.value?.cameraPosition?.target?.longitude ?: 0.0,
            phone = phone.text.toString(),
            website = website.text.toString(),
            openingHours = opening_hours.text.toString()
        )
    }

    companion object {
        private const val PLACE_EXTRA = "place"
        private const val CAMERA_POSITION_EXTRA = "camera_position"

        private const val REQUEST_PICK_LOCATION = 10

        private const val DEFAULT_ZOOM = 13f

        fun startForResult(
            activity: Activity,
            place: Place,
            cameraPosition: CameraPosition,
            requestCode: Int
        ) {
            val intent = Intent(activity, EditPlaceActivity::class.java)
            intent.putExtra(PLACE_EXTRA, place)
            intent.putExtra(CAMERA_POSITION_EXTRA, cameraPosition)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}