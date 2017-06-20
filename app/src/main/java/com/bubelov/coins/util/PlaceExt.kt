package com.bubelov.coins.util

import android.location.Location
import com.bubelov.coins.model.Place

/**
 * @author Igor Bubelov
 */

fun Place.distanceTo(target: Location): Double {
    return DistanceUtils.getDistance(this.latitude, this.longitude, target.latitude, target.longitude)
}