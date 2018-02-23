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

package com.bubelov.coins.util

import android.location.Location
import com.google.android.gms.maps.model.LatLng

fun Location.distanceTo(latitude: Double, longitude: Double, units: DistanceUnits): Double {
    val distanceInKilometers = DistanceUtils.getDistance(this.latitude, this.longitude, latitude, longitude) / 1000.0

    return when (units) {
        DistanceUnits.KILOMETERS -> distanceInKilometers
        DistanceUnits.MILES -> DistanceUtils.toMiles(distanceInKilometers)
    }
}

fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)

fun LatLng.toLocation(): Location {
    val location = Location("")
    location.latitude = latitude
    location.longitude = longitude
    return location
}