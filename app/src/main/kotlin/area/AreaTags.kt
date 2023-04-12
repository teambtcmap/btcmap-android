package area

import android.content.res.Resources
import element.name
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import java.util.*

typealias AreaTags = JsonObject

fun AreaTags.name(
    res: Resources,
    locale: Locale = Locale.getDefault(),
): String {
    return name(
        res = res,
        locale = locale,
    )
}

fun AreaTags.polygons(): List<Polygon> {
    val geoFactory = GeometryFactory()

    val res = mutableListOf<Polygon>()

    val geoJson: JsonObject = Json.decodeFromString(this["geo_json"].toString())

    if (geoJson["type"]?.jsonPrimitive?.content == "FeatureCollection") {
        val features = geoJson["features"]!!.jsonArray

        features.forEach { feature ->
            val geometry = feature.jsonObject["geometry"]!!.jsonObject

            if (geometry["type"]?.jsonPrimitive?.content == "MultiPolygon") {
                val coordinates = geometry["coordinates"]!!.jsonArray

                coordinates.map { it.jsonArray }.forEach { polys ->
                    res += geoFactory.createPolygon(polys.first().jsonArray.map {
                        Coordinate(
                            it.jsonArray.first().jsonPrimitive.double,
                            it.jsonArray.last().jsonPrimitive.double,
                        )
                    }.toTypedArray())
                }
            }

            if (geometry["type"]?.jsonPrimitive?.content == "Polygon") {
                val coordinates = geometry["coordinates"]!!.jsonArray.first().jsonArray

                res += geoFactory.createPolygon(coordinates.map {
                    Coordinate(
                        it.jsonArray.first().jsonPrimitive.double,
                        it.jsonArray.last().jsonPrimitive.double,
                    )
                }.toTypedArray())
            }
        }
    }

    if (geoJson["type"]?.jsonPrimitive?.content == "MultiPolygon") {
        val coordinates = geoJson["coordinates"]!!.jsonArray

        coordinates.map { it.jsonArray }.forEach { polys ->
            val firstPoly = polys.first().jsonArray

            res += geoFactory.createPolygon(firstPoly.map {
                Coordinate(
                    it.jsonArray.first().jsonPrimitive.double,
                    it.jsonArray.last().jsonPrimitive.double,
                )
            }.toTypedArray())
        }
    }

    if (geoJson["type"]?.jsonPrimitive?.content == "Polygon") {
        val coordinates = geoJson["coordinates"]!!.jsonArray
            .first().jsonArray
            .map { it.jsonArray }
            .map { Coordinate(it.first().jsonPrimitive.double, it.last().jsonPrimitive.double) }

        res += geoFactory.createPolygon(coordinates.toTypedArray())
    }

    return res
}