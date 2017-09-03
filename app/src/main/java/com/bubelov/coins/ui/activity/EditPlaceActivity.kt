package com.bubelov.coins.ui.activity

import android.app.Activity
import android.app.ProgressDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.view.View

import com.bubelov.coins.R
import com.bubelov.coins.model.Place
import com.bubelov.coins.ui.viewmodel.EditPlaceViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_edit_place.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.indeterminateProgressDialog
import java.util.*

/**
 * @author Igor Bubelov
 */

class EditPlaceActivity : AbstractActivity(), OnMapReadyCallback, EditPlaceViewModel.Callback {
    private lateinit var viewModel: EditPlaceViewModel

    private var map: GoogleMap? = null

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_place)

        viewModel = ViewModelProviders.of(this).get(EditPlaceViewModel::class.java)
        viewModel.init(intent.getLongExtra(ID_EXTRA, -1), this)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
        toolbar.inflateMenu(R.menu.edit_place)

        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_send) {
                submit()
            }

            true
        }

        val place = viewModel.place

        if (place == null) {
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

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        closed_switch.setOnCheckedChangeListener { _, checked ->
            name.isEnabled = !checked
            phone.isEnabled = !checked
            website.isEnabled = !checked
            description.isEnabled = !checked
            opening_hours.isEnabled = !checked
        }

        change_location.setOnClickListener {
            val intent = PickLocationActivity.newIntent(this, if (place == null) null else LatLng(place.latitude, place.longitude), intent.getParcelableExtra(MAP_CAMERA_POSITION_EXTRA))
            startActivityForResult(intent, REQUEST_PICK_LOCATION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_PICK_LOCATION && resultCode == Activity.RESULT_OK) {
            viewModel.pickedLocation = data.getParcelableExtra(PickLocationActivity.LOCATION_EXTRA)
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(viewModel.pickedLocation, DEFAULT_ZOOM))
            change_location.setText(R.string.change_location)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        googleMap.uiSettings.setAllGesturesEnabled(false)

        val place = viewModel.place

        if (place != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(place.latitude, place.longitude), DEFAULT_ZOOM))
        }
    }

    override fun onTaskStarted() {
        progressDialog?.dismiss()
        progressDialog = indeterminateProgressDialog("Uploading changes")
    }

    override fun onTaskStopped() {
        progressDialog?.dismiss()
    }

    override fun onTaskSuccess() {
        setResult(Activity.RESULT_OK)
        supportFinishAfterTransition()
    }

    override fun onTaskFailure() {
        alert(messageResource =  R.string.could_not_add_place).show()
    }

    private fun submit() {
        if (name!!.length() == 0) {
            alert(messageResource =  R.string.name_is_not_specified).show()
            return
        }

        if (viewModel.place == null) {
            if (viewModel.pickedLocation == null) {
                alert(messageResource =  R.string.location_is_not_specified).show()
                return
            }

            viewModel.addPlace(getChangedPlace())
        } else {
            viewModel.updatePlace(getChangedPlace())
        }
    }

    private fun getChangedPlace(): Place {
        return Place(
                id = if (viewModel.place == null) 0 else viewModel.place!!.id,
                name = name!!.text.toString(),
                description = description!!.text.toString(),
                latitude = viewModel.pickedLocation!!.latitude,
                longitude = viewModel.pickedLocation!!.longitude,
                categoryId = if (viewModel.place == null) 0 else viewModel.place!!.categoryId,
                phone = phone!!.text.toString(),
                website = website!!.text.toString(),
                openingHours = opening_hours.text.toString(),
                visible = !closed_switch.isChecked,
                updatedAt = Date(0))
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