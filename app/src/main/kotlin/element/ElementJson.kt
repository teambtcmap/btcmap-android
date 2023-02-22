package element

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.ZonedDateTime

@Serializable
data class ElementJson(
    val id: String,
    val osm_json: JsonObject,
    val tags: JsonObject,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String,
)

fun ElementJson.toElement(): Element {
    val latLon = getLatLon()

    return Element(
        id = id,
        lat = latLon.first,
        lon = latLon.second,
        osmJson = osm_json,
        tags = tags,
        createdAt = ZonedDateTime.parse(created_at),
        updatedAt = ZonedDateTime.parse(updated_at),
        deletedAt = if (deleted_at.isNotBlank()) ZonedDateTime.parse(deleted_at) else null,
    )
}

fun ElementJson.getLatLon(): Pair<Double, Double> {
    val lat: Double
    val lon: Double

    if (osm_json["type"]!!.jsonPrimitive.content == "node") {
        lat = osm_json["lat"]!!.jsonPrimitive.double
        lon = osm_json["lon"]!!.jsonPrimitive.double
    } else {
        val bounds = osm_json["bounds"]!!.jsonObject

        val boundsMinLat = bounds["minlat"]!!.jsonPrimitive.double
        val boundsMinLon = bounds["minlon"]!!.jsonPrimitive.double
        val boundsMaxLat = bounds["maxlat"]!!.jsonPrimitive.double
        val boundsMaxLon = bounds["maxlon"]!!.jsonPrimitive.double

        lat = (boundsMinLat + boundsMaxLat) / 2.0
        lon = (boundsMinLon + boundsMaxLon) / 2.0
    }

    return Pair(lat, lon)
}