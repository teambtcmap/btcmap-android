package com.bubelov.coins.ui.viewmodel

import android.Manifest
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.content.ContextCompat
import com.bubelov.coins.App
import com.bubelov.coins.Constants
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.model.NotificationArea
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.area.NotificationAreaRepository
import com.bubelov.coins.repository.notification.PlaceNotificationsRepository
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository
import com.bubelov.coins.repository.placecategory.marker.PlaceCategoriesMarkersRepository
import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.ui.model.PlaceMarker
import com.bubelov.coins.util.Analytics
import com.bubelov.coins.util.openUrl
import com.bubelov.coins.util.toLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.ArrayList
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * @author Igor Bubelov
 */

class MainViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    internal lateinit var userRepository: UserRepository

    @Inject
    internal lateinit var notificationAreaRepository: NotificationAreaRepository

    @Inject
    internal lateinit var placesRepository: PlacesRepository

    @Inject
    internal lateinit var placeNotificationsRepository: PlaceNotificationsRepository

    @Inject
    internal lateinit var placeCategoriesRepository: PlaceCategoriesRepository

    @Inject
    internal lateinit var placeCategoriesMarkersRepository: PlaceCategoriesMarkersRepository

    var mapBounds: LatLngBounds by Delegates.observable(LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0)), { _, _, _ -> onMapBoundsChanged() })

    val placeMarkers = MutableLiveData<Collection<PlaceMarker>>()

    var selectedPlace: Place? = null

    val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplication<App>())

    var callback: Callback? = null

    init {
        Injector.mainComponent.inject(this)
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

    fun onSearchClick() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback?.startSearch(null)
            return
        }

        locationClient.lastLocation.addOnCompleteListener { task ->
            callback?.startSearch(task.result)
        }
    }

    fun onDrawerHeaderClick() {
        if (userRepository.signedIn()) {
            callback?.showUserProfile()
        } else {
            callback?.signIn()
        }
    }

    fun clearPlaceNotifications() {
        placeNotificationsRepository.clear()
    }

    fun selectPlace(id: Long) {
        val selectedPlace = placesRepository.getPlace(id)

        if (selectedPlace != null) {
            this.selectedPlace = selectedPlace
            Analytics.logSelectContent(selectedPlace.id.toString(), selectedPlace.name, "place")
        }
    }

    fun onMapBoundsChanged() {
        placeMarkers.value = toPlaceMarkers(placesRepository.getPlaces(mapBounds))
    }

    fun locateUser() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.lastLocation.addOnCompleteListener { task ->
                if (task.result != null) {
                    callback?.moveToLocation(task.result.toLatLng())
                } else {
                    callback?.moveToLocation(LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE))
                }
            }
        } else {
            callback?.requestLocationPermissions()
        }
    }

    fun onLocationPermissionGranted() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationClient.lastLocation.addOnCompleteListener { task ->
            val location = task.result

            if (notificationAreaRepository.notificationArea == null) {
                val area = NotificationArea(
                        location.latitude,
                        location.longitude,
                        Constants.DEFAULT_NOTIFICATION_AREA_RADIUS_METERS
                )

                notificationAreaRepository.notificationArea = area
            }
        }
    }

    fun clearSelection() {
        selectedPlace = null
    }

    fun onSelectedPlaceDetailsClick() {
        Analytics.logViewContent(selectedPlace!!.id.toString(), selectedPlace!!.name, "place")
    }

    fun onSupportChatClick() {
        getApplication<App>().openUrl("https://t.me/joinchat/AAAAAAwVT4aVBdFzcKKbsw")
        Analytics.logSelectContent("chat", null, "screen")
    }

    private fun toPlaceMarkers(places: Collection<Place>): Collection<PlaceMarker> {
        val placeMarkers = ArrayList<PlaceMarker>()

        for ((id, _, _, latitude, longitude, categoryId) in places) {
            placeMarkers.add(PlaceMarker(id, placeCategoriesMarkersRepository.getPlaceCategoryMarker(categoryId), latitude, longitude))
        }

        return placeMarkers
    }

    interface Callback {
        fun signIn()

        fun addPlace()

        fun editPlace(place: Place)

        fun startSearch(location: Location?)

        fun showUserProfile()

        fun selectPlace(place: Place)

        fun requestLocationPermissions()

        fun moveToLocation(location: LatLng)
    }
}