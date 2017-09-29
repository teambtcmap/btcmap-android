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

/**
 * @author Igor Bubelov
 */

class LocationLiveData(context: Context) : LiveData<Location>() {
    private val locationProvider = LocationServices.getFusedLocationProviderClient(context)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult.lastLocation != null) {
                value = locationResult.lastLocation
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActive() {
        locationProvider.requestLocationUpdates(LocationRequest.create(), locationCallback, Looper.getMainLooper())
    }

    override fun onInactive() {
        locationProvider.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    fun requestLastLocation() {
        locationProvider.lastLocation.addOnCompleteListener {
            if (it.result != null) {
                value = it.result
            }
        }
    }
}