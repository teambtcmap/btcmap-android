package com.bubelov.coins.repository.place

import com.bubelov.coins.model.Place
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesDataSourceRam @Inject
internal constructor() {
    internal var places: MutableList<Place> = ArrayList()

    fun getPlaces(bounds: LatLngBounds): List<Place> {
        return places.filter { bounds.contains(LatLng(it.latitude, it.longitude)) }
    }

    fun getPlace(id: Long): Place? {
        return places.firstOrNull { it.id == id }
    }
}