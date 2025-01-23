package area

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

fun AreaTags.polygons(): List<Polygon> {
    val res = mutableListOf<Polygon>()

    val geoJson = this["geo_json"]?.jsonObject!!

    if (geoJson["type"]?.jsonPrimitive?.contentOrNull == "FeatureCollection") {
        val features = geoJson["features"]?.jsonArray!!

        features.forEach { feature ->
            val geometry = feature.jsonObject["geometry"]?.jsonObject!!

            if (geometry["type"]?.jsonPrimitive?.contentOrNull == "MultiPolygon") {
                val coordinates = geometry["coordinates"]?.jsonArray!!

                coordinates.map { polys -> polys.jsonArray.map { it.jsonArray } }.forEach { polys ->
                    res += Polygon.fromLngLats(listOf(polys.first().jsonArray.map { it.jsonArray }
                        .map {
                            Point.fromLngLat(
                                it[0].jsonPrimitive.double,
                                it[1].jsonPrimitive.double,
                            )
                        }))
                }
            }

            if (geometry["type"]?.jsonPrimitive?.contentOrNull == "Polygon") {
                val coordinates =
                    geometry["coordinates"]?.jsonArray!![0].jsonArray.map { it.jsonArray }

                val lngLats = coordinates.map {
                    Point.fromLngLat(
                        it[0].jsonPrimitive.double,
                        it[1].jsonPrimitive.double,
                    )
                }

                res += Polygon.fromLngLats(listOf(lngLats))
            }
        }
    }

    if (geoJson["type"]?.jsonPrimitive?.contentOrNull == "MultiPolygon") {
        val coordinates = geoJson["coordinates"]?.jsonArray!!.map { it.jsonArray }

        coordinates.forEach { polys ->
            val firstPoly = polys.map { it.jsonArray }.first().map { it.jsonArray }

            res += Polygon.fromLngLats(listOf(firstPoly.map {
                Point.fromLngLat(
                    it[0].jsonPrimitive.double,
                    it[1].jsonPrimitive.double,
                )
            }))
        }
    }

    if (geoJson["type"]?.jsonPrimitive?.contentOrNull == "Polygon") {
        val coordinates = geoJson["coordinates"]?.jsonArray!!.map { it.jsonArray }
            .first()
            .map { it.jsonArray }
            .map { Point.fromLngLat(it[0].jsonPrimitive.double, it[1].jsonPrimitive.double) }

        res += Polygon.fromLngLats(listOf(coordinates))
    }

    return res
}