package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.ArrayList
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * @author Igor Bubelov
 */

class MainViewModel(application: Application) : AndroidViewModel(application) {
    @Inject internal lateinit var userRepository: UserRepository

    @Inject internal lateinit var notificationAreaRepository: NotificationAreaRepository

    @Inject internal lateinit var placesRepository: PlacesRepository

    @Inject internal lateinit var placeMarkersRepository: PlaceMarkersRepository

    @Inject internal lateinit var analytics: Analytics

    var mapBounds: LatLngBounds by Delegates.observable(LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0)), { _, _, _ ->
        reloadMarkers()
    })

    val placeMarkers = MutableLiveData<Collection<PlaceMarker>>()

    val selectedPlace: MutableLiveData<Place> = MutableLiveData()

    val location = LocationLiveData(application)

    var moveToNextLocation = true

    var callback: Callback? = null

    init {
        Injector.appComponent.inject(this)
    }

    fun reloadMarkers() {
        placeMarkers.value = toPlaceMarkers(placesRepository.getPlaces(mapBounds))
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

    fun selectPlace(id: Long) {
        selectedPlace.value = placesRepository.getPlace(id)
        analytics.logSelectContent(id.toString(), selectedPlace.value!!.name, "place")
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

    fun clearSelection() {
        selectedPlace.value = null
    }

    fun onSelectedPlaceDetailsClick() {
        analytics.logViewContent(selectedPlace.value!!.id.toString(), selectedPlace.value!!.name, "place")
    }

    private fun toPlaceMarkers(places: Collection<Place>): Collection<PlaceMarker> {
        return places.mapTo(ArrayList()) {
            PlaceMarker(
                placeId = it.id,
                icon = placeMarkersRepository.getPlaceCategoryMarker(it.category),
                latitude = it.latitude,
                longitude = it.longitude
            )
        }
    }

    interface Callback {
        fun signIn()

        fun addPlace()

        fun editPlace(place: Place)

        fun showUserProfile()

        fun selectPlace(place: Place)
    }
}