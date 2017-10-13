package com.bubelov.coins.util

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import timber.log.Timber

/**
 * @author Igor Bubelov
 */

@SuppressLint("MissingPermission")
class LocationLiveData(context: Context, updateIntervalMillis: Long) : LiveData<Location>() {
    private val locationProvider = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = updateIntervalMillis
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            Timber.d("New location: $result")
            val location = result?.lastLocation

            if (location != null) {
                value = location
            }
        }
    }

    init {
        Timber.d("Requesting last location")
        locationProvider.lastLocation.addOnCompleteListener {
            Timber.d("Last location is ${it.result}")
            Timber.d("LiveData value is $value")

            if (value == null && it.result != null) {
                value = it.result
            }
        }
    }

    override fun onActive() {
        Timber.d("Active")
        locationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onInactive() {
        Timber.d("Inactive")
        locationProvider.removeLocationUpdates(locationCallback)
    }
}