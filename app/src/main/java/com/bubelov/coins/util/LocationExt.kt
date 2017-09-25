package com.bubelov.coins.util

import android.location.Location
import com.google.android.gms.maps.model.LatLng

/**
 * @author Igor Bubelov
 */

fun Location.distanceTo(latitude: Double, longitude: Double, units: DistanceUnits): Double {
    val distanceInKilometers = DistanceUtils.getDistance(this.latitude, this.longitude, latitude, longitude) / 1000.0

    return when (units) {
        DistanceUnits.KILOMETERS -> distanceInKilometers
        DistanceUnits.MILES -> DistanceUtils.toMiles(distanceInKilometers)
    }
}

fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)