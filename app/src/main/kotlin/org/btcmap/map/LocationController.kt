package org.btcmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.maps.MapView

class LocationController(private val mapView: MapView) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingZoom: Job? = null

    @Volatile private var activated = false

    /**
     * Set when the user wants a zoom but the location component isn't activated
     * yet. Cleared and acted on after activation completes.
     */
    private var deferredZoom = false

    @SuppressLint("MissingPermission")
    fun onPermissionGranted(context: Context, animateToFirstKnown: Boolean) {
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                if (!activated) {
                    val options =
                        LocationComponentOptions.builder(context).pulseEnabled(true).build()
                    val activation = LocationComponentActivationOptions.builder(context, style)
                        .locationComponentOptions(options)
                        .useDefaultLocationEngine(true)
                        .locationEngineRequest(
                            LocationEngineRequest.Builder(750)
                                .setFastestInterval(750)
                                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                                .build()
                        ).build()
                    map.locationComponent.activateLocationComponent(activation)
                    map.locationComponent.isLocationComponentEnabled = true
                    activated = true
                }

                if (animateToFirstKnown || deferredZoom) {
                    deferredZoom = false
                    zoomToLastKnown()
                }
            }
        }
    }

    fun zoomToLastKnown() {
        if (!activated) {
            deferredZoom = true
            return
        }
        pendingZoom?.cancel()
        pendingZoom = scope.launch {
            val location = withTimeoutOrNull(MAX_WAIT_MS) {
                val cached = readCachedLocation()
                if (cached != null) cached else awaitFreshFix()
            }
            if (location != null) animateTo(location)
        }
    }

    fun destroy() {
        pendingZoom?.cancel()
        scope.cancel()
    }

    private suspend fun readCachedLocation(): Location? = withContext(Dispatchers.Main.immediate) {
        val map = awaitMap() ?: return@withContext null
        try {
            map.locationComponent.lastKnownLocation
        } catch (_: IllegalStateException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitFreshFix(): Location? = withContext(Dispatchers.Main.immediate) {
        val map = awaitMap() ?: return@withContext null
        val engine = map.locationComponent.locationEngine ?: return@withContext null
        val request = LocationEngineRequest.Builder(750)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val callback = object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult) {
                    cont.resumeWith(Result.success(result.lastLocation))
                    engine.removeLocationUpdates(this)
                }

                override fun onFailure(exception: Exception) {
                    cont.resumeWith(Result.success(null))
                    engine.removeLocationUpdates(this)
                }
            }
            engine.requestLocationUpdates(request, callback, null)
            cont.invokeOnCancellation { engine.removeLocationUpdates(callback) }
        }
    }

    private fun animateTo(location: Location) {
        mapView.getMapAsync { map ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(location), 14.0),
            )
        }
    }

    private suspend fun awaitMap(): org.maplibre.android.maps.MapLibreMap? =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            mapView.getMapAsync { map ->
                cont.resumeWith(Result.success(map))
            }
        }

    private companion object {
        const val MAX_WAIT_MS = 10_000L
    }
}
