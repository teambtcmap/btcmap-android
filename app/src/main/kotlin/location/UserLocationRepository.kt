package location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import db.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class UserLocationRepository(
    private val context: Context,
) {

    companion object {
        const val TAG = "UserLocationRepository"

        val DEFAULT_LOCATION: Location = Location(
            lat = 40.7141667,
            lon = -74.0063889,
        )
    }

    private var requestedLocationUpdates = false

    private val _location = MutableStateFlow(DEFAULT_LOCATION)
    val location = _location.asStateFlow()

    private val listener: LocationListener = LocationListener { onNewLocation(it) }

    fun requestLocationUpdates(): Boolean {
        if (requestedLocationUpdates) {
            return false
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Location permission was not granted")
            return false
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = locationManager.allProviders
        Log.d(TAG, "Available providers: $providers")
        Log.d(TAG, "Some providers might not be enabled")

        if (providers.contains(LocationManager.PASSIVE_PROVIDER)) {
            Log.d(TAG, "Passive provider found, requesting last known location")

            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            Log.d(TAG, "Last known location: $lastKnownLocation")

            if (lastKnownLocation != null) {
                _location.update {
                    Location(
                        lat = lastKnownLocation.latitude,
                        lon = lastKnownLocation.longitude,
                    )
                }
            }
        }

        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "GPS provider found, requesting last known location")

            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            Log.d(TAG, "Last known location: $lastKnownLocation")

            if (lastKnownLocation != null) {
                _location.update {
                    Location(
                        lat = lastKnownLocation.latitude,
                        lon = lastKnownLocation.longitude,
                    )
                }
            }
        }

        Log.d(TAG, "Requesting location updates")

        providers.forEach {
            val enabled = locationManager.isProviderEnabled(it)
            Log.d(TAG, "Provider ${it}.enabled=$enabled")

            if (enabled) {
                locationManager.requestLocationUpdates(
                    it,
                    0,
                    0f,
                    listener,
                )
            }
        }

        requestedLocationUpdates = true

        return true
    }

    private fun onNewLocation(androidLocation: AndroidLocation) {
        Log.d(TAG, "Got new location: ${androidLocation.latitude},${androidLocation.longitude}")
        _location.update { androidLocation.toLocation() }
    }

    private fun AndroidLocation.toLocation() = Location(lat = latitude, lon = longitude)
}