package com.bubelov.coins.util

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class LocationLiveData @Inject constructor(private val context: Context) :
    LiveData<Location>() {
    private val locationProvider = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = TimeUnit.SECONDS.toMillis(1)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            val location = result?.lastLocation

            if (location != null) {
                value = location
            }
        }
    }

    init {
        if (hasLocationPermission()) {
            onLocationPermissionGranted()
        }
    }

    override fun onActive() {
        if (hasLocationPermission()) {
            locationProvider.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onInactive() {
        locationProvider.removeLocationUpdates(locationCallback)
    }

    fun hasLocationPermission(): Boolean {
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return permission == PackageManager.PERMISSION_GRANTED
    }

    fun onLocationPermissionGranted() {
        locationProvider.lastLocation.addOnCompleteListener {
            if (value == null && it.result != null) {
                value = it.result
            }
        }

        if (hasActiveObservers()) {
            locationProvider.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }
}