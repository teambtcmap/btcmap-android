package com.bubelov.coins.util

import android.location.Location

import com.bubelov.coins.model.Place

import java.util.Comparator

/**
 * @author Igor Bubelov
 */

class DistanceComparator(private val target: Location) : Comparator<Place> {
    override fun compare(place1: Place, place2: Place): Int {
        return place1.distanceTo(target).compareTo(place2.distanceTo(target))
    }

    fun Place.distanceTo(target: Location): Double {
        return DistanceUtils.getDistance(this.latitude, this.longitude, target.latitude, target.longitude)
    }
}