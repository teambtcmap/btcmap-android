package map

import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.geojson.Polygon

fun boundingBox(polygons: List<Polygon>): LatLngBounds {
    val coordinates = polygons.flatMap { it.coordinates().first() }

    val minLat = coordinates.minBy { it.latitude() }.latitude()
    val maxLat = coordinates.maxBy { it.latitude() }.latitude()
    val minLon = coordinates.minBy { it.longitude() }.longitude()
    val maxLon = coordinates.maxBy { it.longitude() }.longitude()

    return LatLngBounds.from(
        latNorth = maxLat,
        lonEast = maxLon,
        latSouth = minLat,
        lonWest = minLon,
    )
}