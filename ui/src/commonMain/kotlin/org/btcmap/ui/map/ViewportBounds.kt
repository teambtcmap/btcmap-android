package org.btcmap.ui.map

import org.maplibre.compose.util.VisibleBounds

/**
 * The viewport expanded so markers just off screen are preloaded, ported from
 * `org.btcmap.map.expand`.
 *
 * Longitude may fall just outside [-180, 180] for a viewport that crosses the
 * antimeridian; use [longitudeRanges] before querying the database, which only
 * holds longitudes in [-180, 180].
 */
data class ViewportBounds(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
) {

    /**
     * The bounds of a rendered map viewport. MapLibre repeats the world
     * horizontally, so [VisibleBounds] carries continuous longitudes that may
     * fall outside [-180, 180]; [expand] and [longitudeRanges] handle them.
     */
    constructor(visible: VisibleBounds) : this(
        south = visible.south,
        north = visible.north,
        west = visible.west,
        east = visible.east,
    )

    companion object {

        fun expand(bounds: ViewportBounds, scaleFactor: Double = 2.0): ViewportBounds {
            val latitudeCenter = (bounds.south + bounds.north) / 2
            val latitudeSpan = (bounds.north - bounds.south) * scaleFactor

            val spansAntimeridian = bounds.east < bounds.west
            val wrapSpan = bounds.east + 360.0 - bounds.west
            val longitudeSpan =
                (if (spansAntimeridian) wrapSpan else bounds.east - bounds.west) * scaleFactor
            val longitudeCenter = if (spansAntimeridian) {
                normalizeLongitude(bounds.west + wrapSpan / 2)
            } else {
                (bounds.west + bounds.east) / 2
            }

            val south = (latitudeCenter - latitudeSpan / 2).coerceAtLeast(-90.0)
            val north = (latitudeCenter + latitudeSpan / 2).coerceAtMost(90.0)

            if (longitudeSpan >= 360.0) {
                return ViewportBounds(south, north, -180.0, 180.0)
            }

            return ViewportBounds(
                south = south,
                north = north,
                west = longitudeCenter - longitudeSpan / 2,
                east = longitudeCenter + longitudeSpan / 2,
            )
        }
    }

    /** Whether a point at [latitude]/[longitude] falls inside this bounds. */
    fun contains(latitude: Double, longitude: Double): Boolean {
        if (latitude < south || latitude > north) return false
        val lon = normalizeLongitude(longitude)
        return longitudeRanges().any { (west, east) -> lon in west..east }
    }

    /**
     * The longitude range(s) that together cover this bounds, split in two when
     * it crosses the antimeridian.
     */
    fun longitudeRanges(): List<Pair<Double, Double>> {
        val west = normalizeLongitude(west)
        val east = normalizeLongitude(east)
        return if (west <= east) {
            listOf(west to east)
        } else {
            listOf(-180.0 to east, west to 180.0)
        }
    }
}

private fun normalizeLongitude(longitude: Double): Double {
    var normalized = longitude
    while (normalized > 180.0) normalized -= 360.0
    while (normalized < -180.0) normalized += 360.0
    return normalized
}
