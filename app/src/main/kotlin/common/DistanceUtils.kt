package common

import android.location.Location

object DistanceUtils {
    private const val KILOMETERS_IN_MILE = 1.60934

    fun getDistance(startLatitude: Double, startLongitude: Double, endLatitude: Double, endLongitude: Double): Double {
        val distance = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance)
        return distance[0].toDouble()
    }

    fun toMiles(kilometers: Double): Double {
        return kilometers / KILOMETERS_IN_MILE
    }
}