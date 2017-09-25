package com.bubelov.coins.util

import android.location.Location
import com.bubelov.coins.model.Place
import com.google.android.gms.maps.model.LatLng

/**
 * @author Igor Bubelov
 */

fun Place.distanceTo(target: Location): Double {
    return DistanceUtils.getDistance(this.latitude, this.longitude, target.latitude, target.longitude)
}

fun Place.toLatLng() = LatLng(latitude, longitude)