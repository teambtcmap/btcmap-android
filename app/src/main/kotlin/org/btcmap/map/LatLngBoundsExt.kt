package org.btcmap.map

import org.maplibre.android.geometry.LatLngBounds

fun LatLngBounds.expand(scaleFactor: Double = 2.0): LatLngBounds {
    val latSpan = latitudeSpan * scaleFactor
    val lonSpan = longitudeSpan * scaleFactor
    val latNorth = (center.latitude + latSpan / 2).coerceAtMost(90.0)
    val latSouth = (center.latitude - latSpan / 2).coerceAtLeast(-90.0)
    return LatLngBounds.from(
        latNorth = latNorth,
        lonEast = center.longitude + lonSpan / 2,
        latSouth = latSouth,
        lonWest = center.longitude - lonSpan / 2,
    )
}

fun LatLngBounds.splitAtAntimeridian(): Pair<Pair<Double, Double>, Pair<Double, Double>?> {
    var west = longitudeWest
    var east = longitudeEast
    if (west > 180.0) {
        west -= 360.0
    }
    if (east > 180.0) {
        east -= 360.0
    }
    return if (west <= east) {
        Pair(west to east, null)
    } else {
        Pair(-180.0 to east, west to 180.0)
    }
}
