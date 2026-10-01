package org.btcmap.db.table.area

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
 * The GeoJSON is walked directly rather than through a geometry library, so the
 * shape stays free of the platform-specific MapLibre types and can be shared.
 * Missing or malformed GeoJSON yields an empty geometry: the area never matches
 * a point instead of crashing the map.
 */
class AreaGeometry private constructor(
    private val polygons: List<PolygonRings>,
    private val lines: List<Ring>,
) {
    /**
     * The total number of ring/line points, used to bound the map's geometry
     * cache by memory rather than by entry count alone.
     */
    val pointCount: Int =
        polygons.sumOf { rings -> rings.sumOf { it.size } } + lines.sumOf { it.size }

    fun contains(lat: Double, lon: Double): Boolean {
        return polygons.any { polygonContains(it, lon, lat) } || lines.any { lineMatches(it, lon, lat) }
    }

    companion object {
        fun parse(geoJson: String?): AreaGeometry {
            val polygons = mutableListOf<PolygonRings>()
            val lines = mutableListOf<Ring>()
            if (geoJson != null) {
                runCatching { collect(JsonParser.parseString(geoJson), polygons, lines) }
            }
            return AreaGeometry(polygons, lines)
        }

        private fun collect(
            element: JsonElement?,
            polygons: MutableList<PolygonRings>,
            lines: MutableList<Ring>,
        ) {
            val obj = element as? JsonObject ?: return
            when (obj.get("type")?.asString) {
                "Feature" -> collect(obj.get("geometry"), polygons, lines)
                "FeatureCollection" ->
                    obj.getAsJsonArray("features")?.forEach { collect(it, polygons, lines) }
                "GeometryCollection" ->
                    obj.getAsJsonArray("geometries")?.forEach { collect(it, polygons, lines) }
                "Polygon" -> polygons.add(rings(obj.getAsJsonArray("coordinates")))
                "MultiPolygon" ->
                    obj.getAsJsonArray("coordinates")?.forEach { polygons.add(rings(it)) }
                "LineString" -> lines.add(ring(obj.getAsJsonArray("coordinates")))
            }
        }

        private fun rings(array: JsonElement?): PolygonRings =
            (array as? Iterable<JsonElement>)?.map { ring(it) }.orEmpty()

        private fun ring(array: JsonElement?): Ring =
            (array as? Iterable<JsonElement>)?.map { position(it) }.orEmpty()

        private fun position(element: JsonElement): Position {
            val coordinates = element.asJsonArray
            return Position(lon = coordinates.get(0).asDouble, lat = coordinates.get(1).asDouble)
        }
    }
}

/** A ring or line: `[[lon, lat], …]`. */
private typealias Ring = List<Position>

/** A polygon's rings: the first is the exterior, the rest are holes. */
private typealias PolygonRings = List<Ring>

/** A single GeoJSON position, in `[lon, lat]` order. */
private data class Position(val lon: Double, val lat: Double)

fun Area.geoJsonGeometry(): AreaGeometry = AreaGeometry.parse(geoJson)

/**
 * Even-odd ray casting. The first ring is the exterior and the rest are holes,
 * so a point inside a hole is outside the polygon.
 */
private fun polygonContains(rings: PolygonRings, lon: Double, lat: Double): Boolean {
    if (rings.isEmpty()) return false
    if (!ringContains(rings.first(), lon, lat)) return false
    return rings.drop(1).none { ringContains(it, lon, lat) }
}

private fun ringContains(ring: Ring, lon: Double, lat: Double): Boolean {
    var inside = false
    var previous = ring.lastOrNull() ?: return false

    for (point in ring) {
        val xi = point.lon
        val yi = point.lat
        val xj = previous.lon
        val yj = previous.lat

        if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) {
            inside = !inside
        }

        previous = point
    }

    return inside
}

/** A line-string area matches a coordinate that lies on one of its segments. */
private fun lineMatches(line: Ring, lon: Double, lat: Double): Boolean {
    for (i in 0 until line.size - 1) {
        if (onSegment(line[i], line[i + 1], lon, lat)) return true
    }
    return false
}

private fun onSegment(a: Position, b: Position, lon: Double, lat: Double): Boolean {
    val cross = (b.lon - a.lon) * (lat - a.lat) - (b.lat - a.lat) * (lon - a.lon)
    if (abs(cross) > 1e-9) return false

    return lon >= minOf(a.lon, b.lon) - 1e-9 && lon <= maxOf(a.lon, b.lon) + 1e-9 &&
        lat >= minOf(a.lat, b.lat) - 1e-9 && lat <= maxOf(a.lat, b.lat) + 1e-9
}
