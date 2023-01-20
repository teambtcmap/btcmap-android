package map

import org.locationtech.jts.geom.Polygon
import org.osmdroid.util.BoundingBox

fun boundingBox(polygons: List<Polygon>): BoundingBox {
    val coordinates = polygons.flatMap { it.coordinates.toList() }

    val minLat = coordinates.minBy { it.y }.y
    val maxLat = coordinates.maxBy { it.y }.y
    val minLon = coordinates.minBy { it.x }.x
    val maxLon = coordinates.maxBy { it.x }.x

    return BoundingBox(
        maxLat,
        maxLon,
        minLat,
        minLon,
    )
}