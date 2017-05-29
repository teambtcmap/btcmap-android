package com.bubelov.coins.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.view.View
import android.widget.Toast

import com.bubelov.coins.R
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.model.Place
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.bubelov.coins.dagger.Injector
import kotlinx.android.synthetic.main.activity_edit_place.*
import java.util.*

/**
 * @author Igor Bubelov
 */

class EditPlaceActivity : AbstractActivity(), OnMapReadyCallback {
    private var placesRepository: PlacesRepository? = null

    private var place: Place? = null

    private var map: GoogleMap? = null

    private var pickedLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placesRepository = Injector.INSTANCE.mainComponent().placesRepository()
        setContentView(R.layout.activity_edit_place)
        ButterKnife.bind(this)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
        toolbar.inflateMenu(R.menu.edit_place)

        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_send) {
                submit()
            }

            true
        }

        place = placesRepository!!.getPlace(intent.getLongExtra(ID_EXTRA, -1))

        if (place == null) {
            toolbar.setTitle(R.string.action_add_place)
            closed_switch.visibility = View.GONE
            change_location.setText(R.string.set_location)
        } else {
            name.setText(place!!.name)
            change_location.setText(R.string.change_location)
            phone.setText(place!!.phone)
            website.setText(place!!.website)
            description.setText(place!!.description)
            opening_hours.setText(place!!.openingHours)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    fun submit() {
        if (place == null) {
            if (name!!.length() == 0) {
                showAlert(R.string.name_is_not_specified)
                return
            }

            if (pickedLocation == null) {
                showAlert(R.string.location_is_not_specified)
                return
            }
        }

        if (place == null) {
            AddPlaceTask().execute()
        } else {
            UpdatePlaceTask().execute()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_PICK_LOCATION && resultCode == Activity.RESULT_OK) {
            pickedLocation = data.getParcelableExtra<LatLng>(PickLocationActivity.LOCATION_EXTRA)
            map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(pickedLocation, DEFAULT_ZOOM))
            change_location.setText(R.string.change_location)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map!!.uiSettings.setAllGesturesEnabled(false)

        if (place != null) {
            map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(place!!.latitude, place!!.longitude), DEFAULT_ZOOM))
        }
    }

    private val editedPlace: Place
        get() = Place(
                id = if (place == null) 0 else place!!.id,
                name = name!!.text.toString(),
                description = description!!.text.toString(),
                latitude = pickedLocation!!.latitude,
                longitude = pickedLocation!!.longitude,
                categoryId = if (place == null) 0 else place!!.categoryId,
                phone = phone!!.text.toString(),
                website = website!!.text.toString(),
                openingHours = opening_hours.text.toString(),
                visible = !closed_switch.isChecked,
                updatedAt = Date(0)
        )

    @OnCheckedChanged(R.id.closed_switch)
    fun onClosedSwitchChanged() {
        val closed = closed_switch.isChecked

        name!!.isEnabled = !closed
        phone!!.isEnabled = !closed
        website!!.isEnabled = !closed
        description!!.isEnabled = !closed
        opening_hours.isEnabled = !closed
    }

    @OnClick(R.id.change_location)
    fun onChangeLocationClick() {
        PickLocationActivity.startForResult(this, if (place == null) null else LatLng(place!!.latitude, place!!.longitude), intent.getParcelableExtra(MAP_CAMERA_POSITION_EXTRA), REQUEST_PICK_LOCATION)
    }

    private inner class AddPlaceTask : AsyncTask<Void, Void, Boolean>() {
        override fun onPreExecute() {
            showProgress()
        }

        override fun doInBackground(vararg args: Void): Boolean? {
            return placesRepository!!.add(editedPlace)
        }

        override fun onPostExecute(success: Boolean?) {
            hideProgress()

            if (success!!) {
                setResult(Activity.RESULT_OK)
                supportFinishAfterTransition()
            } else {
                Toast.makeText(this@EditPlaceActivity, R.string.could_not_add_place, Toast.LENGTH_LONG).show()
            }
        }
    }

    private inner class UpdatePlaceTask : AsyncTask<Void, Void, Boolean>() {
        override fun onPreExecute() {
            showProgress()
        }

        override fun doInBackground(vararg voids: Void): Boolean? {
            return placesRepository!!.update(editedPlace)
        }

        override fun onPostExecute(success: Boolean?) {
            hideProgress()

            if (success!!) {
                setResult(Activity.RESULT_OK)
                supportFinishAfterTransition()
            } else {
                Toast.makeText(this@EditPlaceActivity, R.string.could_not_update_place, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private val ID_EXTRA = "id"

        private val MAP_CAMERA_POSITION_EXTRA = "map_camera_position"

        private val REQUEST_PICK_LOCATION = 10

        private val DEFAULT_ZOOM = 13f

        fun startForResult(activity: Activity, placeId: Long, mapCameraPosition: CameraPosition, requestCode: Int) {
            val intent = Intent(activity, EditPlaceActivity::class.java)
            intent.putExtra(ID_EXTRA, placeId)
            intent.putExtra(MAP_CAMERA_POSITION_EXTRA, mapCameraPosition)
            activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
        }
    }
}