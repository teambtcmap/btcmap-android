package location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocationRepository(
    private val context: Context,
) {

    companion object {
        val DEFAULT_LOCATION: Location = Location(40.7141667, -74.0063889)
    }

    private var requestedLocationUpdates = false

    private val _location = MutableStateFlow(DEFAULT_LOCATION)
    val location = _location.asStateFlow()

    private val listener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            Log.d("LocationRepository", "Location changed")

            _location.update {
                Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String) {

        }

        override fun onProviderDisabled(provider: String) {

        }
    }

    fun requestLocationUpdates(): Boolean {
        if (requestedLocationUpdates) {
            return false
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = locationManager.allProviders
        Log.d("LocationRepository", "Enabled providers: $providers")

        if (providers.contains(LocationManager.PASSIVE_PROVIDER)) {
            Log.d("LocationRepository", "Passive provider found, requesting last known location")

            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            Log.d("LocationRepository", "Last known location: $lastKnownLocation")

            if (lastKnownLocation != null) {
                _location.update {
                    Location(
                        latitude = lastKnownLocation.latitude,
                        longitude = lastKnownLocation.longitude,
                    )
                }
            }
        }

        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            Log.d("LocationRepository", "GPS provider found, requesting last known location")

            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            Log.d("LocationRepository", "Last known location: $lastKnownLocation")

            if (lastKnownLocation != null) {
                _location.update {
                    Location(
                        latitude = lastKnownLocation.latitude,
                        longitude = lastKnownLocation.longitude,
                    )
                }
            }

            Log.d("LocationRepository", "Requesting GPS location updates")

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10_000,
                0f,
                listener,
            )
        }

        requestedLocationUpdates = true

        return true
    }
}