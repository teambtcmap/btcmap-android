package map

import org.locationtech.jts.geom.Polygon
import org.maplibre.android.geometry.LatLngBounds

fun boundingBox(polygons: List<Polygon>): LatLngBounds {
    val coordinates = polygons.flatMap { it.coordinates.toList() }

    val minLat = coordinates.minBy { it.y }.y
    val maxLat = coordinates.maxBy { it.y }.y
    val minLon = coordinates.minBy { it.x }.x
    val maxLon = coordinates.maxBy { it.x }.x

    return LatLngBounds.from(
        latNorth = maxLat,
        lonEast = maxLon,
        latSouth = minLat,
        lonWest = minLon,
    )
}