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
import com.bubelov.coins.repository.placeicon.PlaceIconsRepository
import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.ui.model.PlaceMarker
import com.bubelov.coins.util.Analytics
import com.bubelov.coins.util.LocationLiveData
import com.google.android.gms.maps.model.LatLngBounds
import java.util.ArrayList
import javax.inject.Inject
import android.arch.lifecycle.Transformations
import android.content.SharedPreferences
import com.bubelov.coins.App
import com.bubelov.coins.R
import org.jetbrains.anko.defaultSharedPreferences

/**
 * @author Igor Bubelov
 */

class MainViewModel(val app: Application) : AndroidViewModel(app), SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject internal lateinit var userRepository: UserRepository

    @Inject internal lateinit var notificationAreaRepository: NotificationAreaRepository

    @Inject internal lateinit var placesRepository: PlacesRepository

    @Inject internal lateinit var placeIconsRepository: PlaceIconsRepository

    @Inject internal lateinit var analytics: Analytics

    val selectedPlaceId = MutableLiveData<Long>()

    val selectedPlace: LiveData<Place> = Transformations.switchMap(selectedPlaceId) { placesRepository.getPlace(it) }

    val userLocation = LocationLiveData(app, 1000)

    var moveToNextLocation = true

    var callback: Callback? = null

    var mapBounds = MutableLiveData<LatLngBounds>()

    var selectedCurrency = MutableLiveData<String>()

    private val places: LiveData<List<Place>>
            = Transformations.switchMap(mapBounds) { placesRepository.getPlaces(it) }

    val placeMarkers: LiveData<List<PlaceMarker>> = Transformations.switchMap(places) { places ->
        Transformations.switchMap(selectedCurrency, { currency ->
            MutableLiveData<List<PlaceMarker>>().apply {
                value = places.filter { it.currencies.contains(currency) }.mapTo(ArrayList()) {
                    PlaceMarker(
                            placeId = it.id,
                            icon = placeIconsRepository.getMarker(it.category),
                            latitude = it.latitude,
                            longitude = it.longitude
                    )
                }
            }
        })
    }

    init {
        Injector.appComponent.inject(this)
        updateSelectedCurrency()
        app.defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        getApplication<App>().defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        updateSelectedCurrency()
    }

    private fun updateSelectedCurrency() {
        selectedCurrency.value = app.defaultSharedPreferences.getString(app.getString(R.string.pref_currency_key), "BTC")
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

    interface Callback {
        fun signIn()
        fun addPlace()
        fun editPlace(place: Place)
        fun showUserProfile()
        fun selectPlace(place: Place)
    }
}