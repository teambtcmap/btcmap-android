package org.btcmap.map

import org.maplibre.android.geometry.LatLngBounds

/**
 * Expands the viewport by [scaleFactor] so markers just off screen are
 * preloaded. Longitude is handled the long way around when the viewport crosses
 * the antimeridian, where MapLibre reports `east < west` and a negative
 * longitude span; a plain centre/span would put the centre on the wrong side
 * and could build an invalid bounds.
 */
fun LatLngBounds.expand(scaleFactor: Double = 2.0): LatLngBounds {
    val latSpan = latitudeSpan * scaleFactor
    val latNorth = (center.latitude + latSpan / 2).coerceAtMost(90.0)
    val latSouth = (center.latitude - latSpan / 2).coerceAtLeast(-90.0)

    val wrapSpan = longitudeEast + 360.0 - longitudeWest
    val spansAntimeridian = longitudeEast < longitudeWest
    val lonSpan = (if (spansAntimeridian) wrapSpan else longitudeSpan) * scaleFactor
    val lonCenter = if (spansAntimeridian) {
        normalizeLongitude(longitudeWest + wrapSpan / 2)
    } else {
        center.longitude
    }

    if (lonSpan >= 360.0) {
        return LatLngBounds.from(
            latNorth = latNorth,
            lonEast = 180.0,
            latSouth = latSouth,
            lonWest = -180.0,
        )
    }

    return LatLngBounds.from(
        latNorth = latNorth,
        lonEast = lonCenter + lonSpan / 2,
        latSouth = latSouth,
        lonWest = lonCenter - lonSpan / 2,
    )
}

/**
 * Splits a bounds into the two longitude ranges that together cover it, when it
 * crosses the antimeridian, so the two halves can be queried separately. A
 * bounds that does not cross is returned as-is with a null second range.
 *
 * Either edge can be reported just outside [-180, 180] after [expand], so both
 * are normalised before the crossing is detected.
 */
fun LatLngBounds.splitAtAntimeridian(): Pair<Pair<Double, Double>, Pair<Double, Double>?> {
    val west = normalizeLongitude(longitudeWest)
    val east = normalizeLongitude(longitudeEast)
    return if (west <= east) {
        Pair(west to east, null)
    } else {
        Pair(-180.0 to east, west to 180.0)
    }
}

private fun normalizeLongitude(longitude: Double): Double {
    var normalized = longitude
    while (normalized > 180.0) normalized -= 360.0
    while (normalized < -180.0) normalized += 360.0
    return normalized
}

/**
 * Runs [fetch] over this bounds' longitude range(s), splitting it in two when
 * the viewport crosses the antimeridian. The database only holds longitudes in
 * [-180, 180], so the wrapped half has to be queried as its own range and the
 * results merged.
 */
internal fun <T> LatLngBounds.queryByBounds(
    fetch: (minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) -> List<T>,
): List<T> {
    val (first, second) = splitAtAntimeridian()
    val head = fetch(latitudeSouth, latitudeNorth, first.first, first.second)
    if (second == null) return head
    return head + fetch(latitudeSouth, latitudeNorth, second.first, second.second)
}
