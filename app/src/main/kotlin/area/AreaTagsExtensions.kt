package area

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon

fun AreaTags.polygons(): List<Polygon> {
    val geoFactory = GeometryFactory()

    val res = mutableListOf<Polygon>()

    val geoJson = this["geo_json"]?.jsonObject!!

    if (geoJson["type"]?.jsonPrimitive?.contentOrNull == "FeatureCollection") {
        val features = geoJson["features"]?.jsonArray!!

        features.forEach { feature ->
            val geometry = feature.jsonObject["geometry"]?.jsonObject!!

            if (geometry["type"]?.jsonPrimitive?.contentOrNull == "MultiPolygon") {
                val coordinates = geometry["coordinates"]?.jsonArray!!

                coordinates.map { polys -> polys.jsonArray.map { it.jsonArray } }.forEach { polys ->
                    res += geoFactory.createPolygon(polys.first().jsonArray.map { it.jsonArray }
                        .map {
                            Coordinate(
                                it[0].jsonPrimitive.double,
                                it[1].jsonPrimitive.double,
                            )
                        }.toTypedArray())
                }
            }

            if (geometry["type"]?.jsonPrimitive?.contentOrNull == "Polygon") {
                val coordinates =
                    geometry["coordinates"]?.jsonArray!![0].jsonArray.map { it.jsonArray }

                res += geoFactory.createPolygon(coordinates.map {
                    Coordinate(
                        it[0].jsonPrimitive.double,
                        it[1].jsonPrimitive.double,
                    )
                }.toTypedArray())
            }
        }
    }

    if (geoJson["type"]?.jsonPrimitive?.contentOrNull == "MultiPolygon") {
        val coordinates = geoJson["coordinates"]?.jsonArray!!.map { it.jsonArray }

        coordinates.forEach { polys ->
            val firstPoly = polys.map { it.jsonArray }.first().map { it.jsonArray }

            res += geoFactory.createPolygon(firstPoly.map {
                Coordinate(
                    it[0].jsonPrimitive.double,
                    it[1].jsonPrimitive.double,
                )
            }.toTypedArray())
        }
    }

    if (geoJson["type"]?.jsonPrimitive?.contentOrNull == "Polygon") {
        val coordinates = geoJson["coordinates"]?.jsonArray!!.map { it.jsonArray }
            .first()
            .map { it.jsonArray }
            .map { Coordinate(it[0].jsonPrimitive.double, it[1].jsonPrimitive.double) }

        res += geoFactory.createPolygon(coordinates.toTypedArray())
    }

    return res
}