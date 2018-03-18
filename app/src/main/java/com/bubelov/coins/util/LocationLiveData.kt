package com.bubelov.coins.util

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import com.bubelov.coins.model.Location
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationLiveData @Inject constructor(
    private val context: Context,
    private val permissionChecker: PermissionChecker
) : LiveData<Location>() {
    private val locationManager by lazy { context.getSystemService(LOCATION_SERVICE) as LocationManager }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            Timber.d("onLocationChanged(location: $location)")
            value = location.toLocation()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {
            val statusString = when (status) {
                LocationProvider.OUT_OF_SERVICE -> "OUT_OF_SERVICE"
                LocationProvider.TEMPORARILY_UNAVAILABLE -> "TEMPORARILY_UNAVAILABLE"
                LocationProvider.AVAILABLE -> "AVAILABLE"
                else -> "UNKNOWN"
            }

            Timber.d("onStatusChanged(provider: $provider, status: $statusString, extras: $extras)")
        }

        override fun onProviderEnabled(provider: String) {
            Timber.d("onProviderEnabled(provider: $provider)")
        }

        override fun onProviderDisabled(provider: String) {
            Timber.d("onProviderDisabled(provider: $provider")
        }
    }

    init {
        if (isLocationPermissionGranted()) {
            onLocationPermissionGranted()
        } else {
            value = null
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActive() {
        if (isLocationPermissionGranted()) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_UPDATE_TIME_MILLIS,
                MIN_UPDATE_DISTANCE_METERS,
                locationListener
            )
        }
    }

    override fun onInactive() {
        locationManager.removeUpdates(locationListener)
    }

    fun isLocationPermissionGranted(): Boolean {
        val checkResult = permissionChecker.check(Manifest.permission.ACCESS_FINE_LOCATION)
        return checkResult == PermissionChecker.CheckResult.GRANTED
    }

    fun onLocationPermissionGranted() {
        if (hasActiveObservers()) {
            onActive()
        }
    }

    companion object {
        private const val MIN_UPDATE_TIME_MILLIS = 3000L
        private const val MIN_UPDATE_DISTANCE_METERS = 1f
    }
}