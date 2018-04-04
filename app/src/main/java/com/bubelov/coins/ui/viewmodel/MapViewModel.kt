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

package com.bubelov.coins.ui.viewmodel

import android.arch.lifecycle.*
import com.bubelov.coins.model.Location
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
import com.bubelov.coins.util.SelectedCurrencyLiveData

class MapViewModel @Inject constructor(
    private val notificationAreaRepository: NotificationAreaRepository,
    private val placesRepository: PlacesRepository,
    private val placeIconsRepository: PlaceIconsRepository,
    private val location: LocationLiveData,
    val userRepository: UserRepository,
    val analytics: Analytics,
    val selectedCurrency: SelectedCurrencyLiveData
) : ViewModel() {

    val selectedPlaceId = MutableLiveData<Long>()
    val selectedPlace: LiveData<Place> = Transformations.switchMap(selectedPlaceId) { placesRepository.find(it) }
    var navigateToNextSelectedPlace = false

    var callback: Callback? = null

    var mapBounds = MutableLiveData<LatLngBounds>()

    private val places: LiveData<List<Place>> =
        Transformations.switchMap(mapBounds) { placesRepository.getPlaces(it) }

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

    val userLocation: LiveData<Location> = Transformations.map(location, { location ->
        if (location != null && notificationAreaRepository.notificationArea == null) {
            notificationAreaRepository.notificationArea = NotificationArea(
                location.latitude,
                location.longitude,
                NotificationAreaRepository.DEFAULT_RADIUS_METERS
            )
        }

        location
    })

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

    fun isLocationPermissionGranted() = location.isLocationPermissionGranted()

    fun onLocationPermissionGranted() = location.onLocationPermissionGranted()

    interface Callback {
        fun signIn()
        fun addPlace()
        fun editPlace(place: Place)
        fun showUserProfile()
    }
}