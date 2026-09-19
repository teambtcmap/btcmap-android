package org.btcmap.offline

/**
 * A geographic bounding box in degrees, ordered the way the area cache stores
 * it: `[west, south, east, north]`.
 *
 * Kept independent of MapLibre so the download estimates stay unit testable.
 */
internal data class OfflineBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
)
