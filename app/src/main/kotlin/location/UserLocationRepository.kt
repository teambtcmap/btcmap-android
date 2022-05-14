package location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
        val DEFAULT_LOCATION: Location = Location(
            lat = 40.7141667,
            lon = -74.0063889,
        )
    }

    private var requestedLocationUpdates = false

    private val _location = MutableStateFlow(DEFAULT_LOCATION)
    val location = _location.asStateFlow()

    private val listener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            Log.d("UserLocationRepository", "Location changed")

            _location.update {
                Location(
                    lat = location.latitude,
                    lon = location.longitude,
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
        Log.d("UserLocationRepository", "Enabled providers: $providers")

        if (providers.contains(LocationManager.PASSIVE_PROVIDER)) {
            Log.d("UserLocationRepository", "Passive provider found, requesting last known location")

            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            Log.d("UserLocationRepository", "Last known location: $lastKnownLocation")

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
            Log.d("UserLocationRepository", "GPS provider found, requesting last known location")

            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            Log.d("UserLocationRepository", "Last known location: $lastKnownLocation")

            if (lastKnownLocation != null) {
                _location.update {
                    Location(
                        lat = lastKnownLocation.latitude,
                        lon = lastKnownLocation.longitude,
                    )
                }
            }

            Log.d("UserLocationRepository", "Requesting GPS location updates")

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