package com.bubelov.coins.ui.viewmodel

import android.Manifest
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
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
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import timber.log.Timber
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

    val googleApiClient = GoogleApiClient.Builder(getApplication())
            .addApi(LocationServices.API)
            .addApi(Auth.GOOGLE_SIGN_IN_API)
            .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    locateUser()
                }

                override fun onConnectionSuspended(p0: Int) {
                    Timber.e("Connection suspended")
                }
            })
            .build()

    lateinit var callback: Callback

    init {
        Injector.INSTANCE.mainComponent().inject(this)
    }

    fun onAddPlaceClick() {
        if (userRepository.signedIn()) {
            callback.addPlace()
        } else {
            callback.signIn()
        }
    }

    fun onEditPlaceClick(place: Place) {
        if (userRepository.signedIn()) {
            callback.editPlace(place)
        } else {
            callback.signIn()
        }
    }

    fun onSearchClick() {
        callback.startSearch(getLastLocation())
    }

    fun onDrawerHeaderClick() {
        if (userRepository.signedIn()) {
            callback.showUserProfile()
        } else {
            callback.signIn()
        }
    }

    fun clearPlaceNotifications() {
        placeNotificationsRepository.clear()
    }

    fun selectPlace(id: Long) {
        val selectedPlace = placesRepository.getPlace(id)

        if (selectedPlace != null) {
            this.selectedPlace = selectedPlace
            Analytics.logSelectContentEvent(selectedPlace.id.toString(), selectedPlace.name, "place")
        }
    }

    fun onMapBoundsChanged() {
        placeMarkers.value = toPlaceMarkers(placesRepository.getPlaces(mapBounds))
    }

    fun locateUser() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lastLocation = getLastLocation()

            if (lastLocation != null) {
                callback.moveToLocation(lastLocation.toLatLng())
            } else {
                callback.moveToLocation(LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE))
            }
        } else {
            callback.requestLocationPermissions()
        }
    }

    fun getLastLocation(): Location? {
        if (googleApiClient.isConnected && ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)

            if (location != null) {
                onNewLocation(location)
            }

            return location
        } else {
            return null
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

    fun clearSelection() {
        selectedPlace = null
    }

    fun onSelectedPlaceDetailsClick() {
        Analytics.logViewContentEvent(selectedPlace!!.id.toString(), selectedPlace!!.name, "place")
    }

    fun onSupportChatClick() {
        getApplication<App>().openUrl("https://t.me/joinchat/AAAAAAwVT4aVBdFzcKKbsw")
        Analytics.logSelectContentEvent("chat", null, "screen")
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