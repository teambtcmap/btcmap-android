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
    val osmJson: JsonObject,
    val tags: JsonObject,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String,
)

fun ElementJson.toElement(): Element {
    val latLon = getLatLon()

    return Element(
        id = id,
        lat = latLon.first,
        lon = latLon.second,
        osmJson = osmJson,
        tags = tags,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(updatedAt),
        deletedAt = if (deletedAt.isNotBlank()) ZonedDateTime.parse(deletedAt) else null,
    )
}

fun ElementJson.getLatLon(): Pair<Double, Double> {
    val lat: Double
    val lon: Double

    if (osmJson["type"]!!.jsonPrimitive.content == "node") {
        lat = osmJson["lat"]!!.jsonPrimitive.double
        lon = osmJson["lon"]!!.jsonPrimitive.double
    } else {
        val bounds = osmJson["bounds"]!!.jsonObject

        val boundsMinLat = bounds["minlat"]!!.jsonPrimitive.double
        val boundsMinLon = bounds["minlon"]!!.jsonPrimitive.double
        val boundsMaxLat = bounds["maxlat"]!!.jsonPrimitive.double
        val boundsMaxLon = bounds["maxlon"]!!.jsonPrimitive.double

        lat = (boundsMinLat + boundsMaxLat) / 2.0
        lon = (boundsMinLon + boundsMaxLon) / 2.0
    }

    return Pair(lat, lon)
}