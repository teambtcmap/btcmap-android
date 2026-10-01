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

    companion object {

        fun expand(visible: VisibleBounds, scaleFactor: Double = 2.0): ViewportBounds {
            val latitudeCenter = (visible.south + visible.north) / 2
            val latitudeSpan = (visible.north - visible.south) * scaleFactor

            val spansAntimeridian = visible.east < visible.west
            val wrapSpan = visible.east + 360.0 - visible.west
            val longitudeSpan =
                (if (spansAntimeridian) wrapSpan else visible.east - visible.west) * scaleFactor
            val longitudeCenter = if (spansAntimeridian) {
                normalizeLongitude(visible.west + wrapSpan / 2)
            } else {
                (visible.west + visible.east) / 2
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
