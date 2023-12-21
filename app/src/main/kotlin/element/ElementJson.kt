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
    val latLon = getLatLon()

    return Element(
        id = id,
        osmId = getOsmId(),
        lat = latLon.first,
        lon = latLon.second,
        osmJson = osmData ?: JSONObject(),
        tags = tags ?: JSONObject(),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

fun ElementJson.getLatLon(): Pair<Double, Double> {
    if (osmData == null) {
        return Pair(0.0, 0.0)
    }

    val lat: Double
    val lon: Double

    if (osmData.getString("type") == "node") {
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

    return Pair(lat, lon)
}

fun ElementJson.getOsmId(): String {
    if (osmData == null) {
        return ""
    }

    val type = osmData.optString("type").ifBlank { return "" }
    val id = osmData.optString("id").ifBlank { return "" }

    return "$type:$id"
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