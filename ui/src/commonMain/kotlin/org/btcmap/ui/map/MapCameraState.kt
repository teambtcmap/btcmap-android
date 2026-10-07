package org.btcmap.ui.map

/**
 * The full camera state of the map: where it is looking, how far in, and at what
 * angle. Reported as the camera comes to rest so a host can remember it and
 * reopen there, and handed back to restore it.
 */
data class MapCameraState(
    val lat: Double,
    val lon: Double,
    val zoom: Double,
    /** Rotation in degrees clockwise from north. */
    val bearing: Double,
    /** Pitch in degrees from straight down. */
    val tilt: Double,
)
