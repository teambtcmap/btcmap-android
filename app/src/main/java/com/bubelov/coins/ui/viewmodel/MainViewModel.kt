package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import com.bubelov.coins.Constants
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.model.NotificationArea
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.area.NotificationAreaRepository
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.placemarker.PlaceMarkersRepository
import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.ui.model.PlaceMarker
import com.bubelov.coins.util.Analytics
import com.bubelov.coins.util.LocationLiveData
import com.google.android.gms.maps.model.LatLngBounds
import java.util.ArrayList
import javax.inject.Inject
import android.arch.lifecycle.Transformations

/**
 * @author Igor Bubelov
 */

class MainViewModel(application: Application) : AndroidViewModel(application) {
    @Inject internal lateinit var userRepository: UserRepository

    @Inject internal lateinit var notificationAreaRepository: NotificationAreaRepository

    @Inject internal lateinit var placesRepository: PlacesRepository

    @Inject internal lateinit var placeMarkersRepository: PlaceMarkersRepository

    @Inject internal lateinit var analytics: Analytics

    val selectedPlaceId = MutableLiveData<Long>()

    val selectedPlace: LiveData<Place?>
            = Transformations.switchMap(selectedPlaceId) { placesRepository.getPlace(it) }

    val location = LocationLiveData(application)

    var moveToNextLocation = true

    var callback: Callback? = null

    var mapBounds = MutableLiveData<LatLngBounds>()

    private val places: LiveData<List<Place>>
            = Transformations.switchMap(mapBounds) { placesRepository.getPlaces(it) }

    val placeMarkers: LiveData<List<PlaceMarker>> = Transformations.switchMap(places) {
        MutableLiveData<List<PlaceMarker>>().apply {
            value = it.mapTo(ArrayList()) {
                PlaceMarker(
                        placeId = it.id,
                        icon = placeMarkersRepository.getPlaceCategoryMarker(it.category),
                        latitude = it.latitude,
                        longitude = it.longitude
                )
            }
        }
    }

    init {
        Injector.appComponent.inject(this)
    }

    fun onAddPlaceClick() {
        if (userRepository.signedIn()) {
            callback?.addPlace()
        } else {
            callback?.signIn()
        }
    }

    fun onEditPlaceClick(place: Place) {
        if (userRepository.signedIn()) {
            callback?.editPlace(place)
        } else {
            callback?.signIn()
        }
    }

    fun onDrawerHeaderClick() {
        if (userRepository.signedIn()) {
            callback?.showUserProfile()
        } else {
            callback?.signIn()
        }
    }

    fun onNewLocation(location: Location) {
        if (notificationAreaRepository.notificationArea == null) {
            val area = NotificationArea(
                    location.latitude,
                    location.longitude,
                    Constants.DEFAULT_NOTIFICATION_AREA_RADIUS_METERS
            )

            notificationAreaRepository.notificationArea = area
        }
    }

    fun onSelectedPlaceDetailsClick() {
        analytics.logViewContent(selectedPlace.value!!.id.toString(), selectedPlace.value!!.name, "place")
    }

    interface Callback {
        fun signIn()

        fun addPlace()

        fun editPlace(place: Place)

        fun showUserProfile()

        fun selectPlace(place: Place)
    }
}