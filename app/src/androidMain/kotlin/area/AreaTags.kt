package area

import android.content.res.Resources
import element.name
import json.toList
import json.toListOfArrays
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import java.util.Locale

typealias AreaTags = JSONObject

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

    val geoJson = this.getJSONObject("geo_json")

    if (geoJson.getString("type") == "FeatureCollection") {
        val features = geoJson.getJSONArray("features")

        features.toList().forEach { feature ->
            val geometry = feature.getJSONObject("geometry")

            if (geometry.getString("type") == "MultiPolygon") {
                val coordinates = geometry.getJSONArray("coordinates").toList()

                coordinates.map { JSONArray(it).toListOfArrays() }.forEach { polys ->
                    res += geoFactory.createPolygon(polys.first().toListOfArrays().map {
                        Coordinate(
                            it.getDouble(0),
                            it.getDouble(1),
                        )
                    }.toTypedArray())
                }
            }

            if (geometry.getString("type") == "Polygon") {
                val coordinates =
                    geometry.getJSONArray("coordinates").getJSONArray(0).toListOfArrays()

                res += geoFactory.createPolygon(coordinates.map {
                    Coordinate(
                        it.getDouble(0),
                        it.getDouble(1),
                    )
                }.toTypedArray())
            }
        }
    }

    if (geoJson.getString("type") == "MultiPolygon") {
        val coordinates = geoJson.getJSONArray("coordinates").toListOfArrays()

        coordinates.forEach { polys ->
            val firstPoly = polys.toListOfArrays().first().toListOfArrays()

            res += geoFactory.createPolygon(firstPoly.map {
                Coordinate(
                    it.getDouble(0),
                    it.getDouble(1),
                )
            }.toTypedArray())
        }
    }

    if (geoJson.getString("type") == "Polygon") {
        val coordinates = geoJson.getJSONArray("coordinates").toListOfArrays()
            .first()
            .toListOfArrays()
            .map { Coordinate(it.getDouble(0), it.getDouble(1)) }

        res += geoFactory.createPolygon(coordinates.toTypedArray())
    }

    return res
}