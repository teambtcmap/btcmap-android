package com.bubelov.coins.util

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.location.Location
import android.location.LocationManager


/**
 * @author Igor Bubelov
 */

class LocationLiveData(context: Context) : LiveData<Location>() {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationListener = object : SimpleLocationListener() {
        override fun onLocationChanged(location: Location) {
            value = location
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActive() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0.toLong(), 0.toFloat(), locationListener)
    }

    override fun onInactive() {
        locationManager.removeUpdates(locationListener)
    }
}