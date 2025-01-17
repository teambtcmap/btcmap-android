package element

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream

data class ElementJson(
    val id: Long,
    val osmData: JSONObject?,
    val tags: JSONObject?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun ElementJson.toElement(): Element {
    val lat: Double
    val lon: Double

    if (osmData!!.getString("type") == "node") {
        lat = osmData.getDouble("lat")
        lon = osmData.getDouble("lon")
    } else {
        val bounds = osmData.getJSONObject("bounds")

        val boundsMinLat = bounds.getDouble("minlat")
        val boundsMinLon = bounds.getDouble("minlon")
        val boundsMaxLat = bounds.getDouble("maxlat")
        val boundsMaxLon = bounds.getDouble("maxlon")

        lat = (boundsMinLat + boundsMaxLat) / 2.0
        lon = (boundsMinLon + boundsMaxLon) / 2.0
    }

    return Element(
        id = id,
        overpassData = osmData,
        tags = tags!!,
        updatedAt = updatedAt,
        lat = lat,
        lon = lon,
    )
}

fun InputStream.toElementsJson(): List<ElementJson> {
    return toJsonArray().map {
        ElementJson(
            id = it.getLong("id"),
            osmData = it.optJSONObject("osm_data"),
            tags = it.optJSONObject("tags"),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.optString("deleted_at").ifBlank { null },
        )
    }
}