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

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.bubelov.coins.model.Location
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class LocationLiveData @Inject constructor(
    private val context: Context,
    private val permissionChecker: PermissionChecker
) : LiveData<Location>() {
    private val locationManager by lazy { context.getSystemService(LOCATION_SERVICE) as LocationManager }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            value = location.toLocation()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String) {

        }

        override fun onProviderDisabled(provider: String) {

        }
    }

    init {
        if (isLocationPermissionGranted()) {
            onLocationPermissionGranted()
        } else {
            value = null
        }
    }

    override fun onActive() {
        if (isLocationPermissionGranted()) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0.0f,
                locationListener
            )

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0.0f,
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
        val lastKnownLocation = getLastKnownLocation()

        if (lastKnownLocation != null) {
            value = lastKnownLocation
        }

        if (hasActiveObservers()) {
            onActive()
        }
    }

    private fun getLastKnownLocation(): Location? {
        val lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        return lastGpsLocation?.toLocation() ?: lastNetworkLocation?.toLocation()
    }
}