package org.btcmap.map

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.db.table.area.Area
import org.btcmap.json.parseJson

/**
 * The area's cached GeoJSON as a GeoJSON FeatureCollection, the shape a MapLibre
 * source expects.
 *
 * The API serializes an area's geometry either bare (a Polygon or MultiPolygon)
 * or already wrapped in a FeatureCollection, and a Feature on its own is
 * possible too, so the top-level `type` decides how it is wrapped. A malformed
 * or absent geometry becomes an empty FeatureCollection rather than failing the
 * caller.
 */
fun Area.toRenderGeoJson(): String {
    val json = geoJson ?: return EMPTY_GEOJSON
    val type = runCatching {
        (parseJson(json) as? JsonObject)?.get("type")?.jsonPrimitive?.content
    }.getOrNull()

    return when (type) {
        "FeatureCollection" -> json
        "Feature" -> """{"type":"FeatureCollection","features":[$json]}"""
        "Point", "MultiPoint", "LineString", "MultiLineString", "Polygon", "MultiPolygon",
        "GeometryCollection",
        -> """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":$json}]}"""

        else -> EMPTY_GEOJSON
    }
}
