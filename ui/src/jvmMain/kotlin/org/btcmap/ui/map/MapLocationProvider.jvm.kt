package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.maplibre.compose.location.LocationAccuracyAuthorization
import org.maplibre.compose.location.LocationBackendAvailability
import org.maplibre.compose.location.LocationEvent
import org.maplibre.compose.location.LocationPermission
import org.maplibre.compose.location.LocationProvider
import org.maplibre.compose.location.LocationRequest
import org.maplibre.compose.location.rememberDefaultLocationProvider

/**
 * The desktop's location provider: the portal-backed default on every platform,
 * with the Linux one wrapped so the map's location button works.
 *
 * The XDG Location portal has no permission-status query, and the library's
 * [LocationProvider.requestPermission] answers it by opening a portal session,
 * starting it, and closing it again. On GeoClue that probe consumes the single
 * initial fix, so the tracking session the app opens afterwards never receives a
 * location: the button then waits forever for a fix that cannot arrive. The
 * wrapper grants the permission itself instead, leaving the session that
 * [LocationProvider.updates] opens to authorize and deliver the fix, which is
 * what the portal does on its own when nothing has probed it first.
 */
@Composable
internal actual fun rememberLocationProvider(): LocationProvider {
    val default = rememberDefaultLocationProvider()
    return remember(default) {
        if (isLinux()) LinuxLocationProvider(default) else default
    }
}

private fun isLinux(): Boolean =
    System.getProperty("os.name").orEmpty().lowercase().startsWith("linux")

/** Wraps the portal provider so its permission request does not probe the portal. */
private class LinuxLocationProvider(
    private val delegate: LocationProvider,
) : LocationProvider {
    private val permissionState = MutableStateFlow<LocationPermission>(
        LocationPermission.NotGranted(canRequest = null),
    )

    override val backendId: String? get() = delegate.backendId

    override val backendAvailability: LocationBackendAvailability
        get() = delegate.backendAvailability

    override val permission: StateFlow<LocationPermission> get() = permissionState

    /**
     * Grants the permission without the library's portal probe. The session
     * [updates] opens is what actually asks GeoClue, so the fix it delivers is
     * not consumed by a throwaway session first.
     */
    override fun requestPermission() {
        permissionState.value = LocationPermission.Granted(LocationAccuracyAuthorization.Unknown)
    }

    override fun updates(request: LocationRequest): Flow<LocationEvent> = delegate.updates(request)

    override fun close() = delegate.close()
}
