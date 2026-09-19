package org.btcmap.db.table.area

import com.google.gson.JsonParser
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Geometry
import org.maplibre.geojson.GeometryCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.abs

/**
 * An area's cached GeoJSON, resolved to the shapes a point-in-area test can
 * use.
 *
 * The server accepts a bare geometry, a feature or a feature collection and
 * tests a coordinate against every polygon, multi-polygon and line string it
 * contains (see `Area::geo_json_geometries`). The same flattening happens here
 * so an area matched locally matches exactly the areas the server would return.
 *
 * Missing or malformed GeoJSON yields an empty geometry: the area never
 * matches a point instead of crashing the map.
 */
internal class AreaGeometry private constructor(
    private val polygons: List<Polygon>,
    private val lines: List<LineString>,
) {
    fun contains(lat: Double, lon: Double): Boolean {
        return polygons.any { it.contains(lon, lat) } || lines.any { it.contains(lon, lat) }
    }

    companion object {
        fun parse(geoJson: String?): AreaGeometry {
            val polygons = mutableListOf<Polygon>()
            val lines = mutableListOf<LineString>()
            if (geoJson != null) {
                runCatching { collect(geoJson, polygons, lines) }
            }
            return AreaGeometry(polygons, lines)
        }

        private fun collect(
            raw: String,
            polygons: MutableList<Polygon>,
            lines: MutableList<LineString>,
        ) {
            val type = JsonParser.parseString(raw).asJsonObject.get("type")?.asString
            when (type) {
                "Feature" -> Feature.fromJson(raw).geometry()?.let { collect(it, polygons, lines) }
                "FeatureCollection" -> FeatureCollection.fromJson(raw).features().orEmpty()
                    .forEach { feature -> feature.geometry()?.let { collect(it, polygons, lines) } }
                "GeometryCollection" -> GeometryCollection.fromJson(raw).geometries().orEmpty()
                    .forEach { collect(it, polygons, lines) }
                "Polygon" -> polygons.add(Polygon.fromJson(raw))
                "MultiPolygon" -> polygons.addAll(MultiPolygon.fromJson(raw).polygons())
                "LineString" -> lines.add(LineString.fromJson(raw))
            }
        }

        private fun collect(
            geometry: Geometry,
            polygons: MutableList<Polygon>,
            lines: MutableList<LineString>,
        ) {
            collect(geometry.toJson(), polygons, lines)
        }
    }
}

internal fun Area.geoJsonGeometry(): AreaGeometry = AreaGeometry.parse(geoJson)

/**
 * Even-odd ray casting. The first ring is the exterior and the rest are holes,
 * so a point inside a hole is outside the polygon.
 */
private fun Polygon.contains(lon: Double, lat: Double): Boolean {
    val rings = coordinates()
    if (rings.isEmpty()) return false
    if (!ringContains(rings.first(), lon, lat)) return false
    return rings.drop(1).none { ringContains(it, lon, lat) }
}

private fun ringContains(ring: List<Point>, lon: Double, lat: Double): Boolean {
    var inside = false
    var previous = ring.lastOrNull() ?: return false

    for (point in ring) {
        val xi = point.longitude()
        val yi = point.latitude()
        val xj = previous.longitude()
        val yj = previous.latitude()

        if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) {
            inside = !inside
        }

        previous = point
    }

    return inside
}

/** A line-string area matches a coordinate that lies on one of its segments. */
private fun LineString.contains(lon: Double, lat: Double): Boolean {
    val points = coordinates()
    for (i in 0 until points.size - 1) {
        if (onSegment(points[i], points[i + 1], lon, lat)) return true
    }
    return false
}

private fun onSegment(a: Point, b: Point, lon: Double, lat: Double): Boolean {
    val ax = a.longitude()
    val ay = a.latitude()
    val bx = b.longitude()
    val by = b.latitude()

    val cross = (bx - ax) * (lat - ay) - (by - ay) * (lon - ax)
    if (abs(cross) > 1e-9) return false

    return lon >= minOf(ax, bx) - 1e-9 && lon <= maxOf(ax, bx) + 1e-9 &&
        lat >= minOf(ay, by) - 1e-9 && lat <= maxOf(ay, by) + 1e-9
}
