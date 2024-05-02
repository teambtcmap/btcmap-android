package element

import android.content.res.Resources
import org.json.JSONObject

data class Element(
    val id: Long,
    val osmId: String,
    val lat: Double,
    val lon: Double,
    val osmJson: JSONObject,
    val tags: JSONObject,
    val updatedAt: String,
    val deletedAt: String?,
)

fun Element.name(res: Resources): String {
    return osmTags().name(res)
}

fun Element.osmTag(name: String): String {
    return osmTags().optString(name, "")
}

fun Element.osmTags(): OsmTags {
    return (osmJson.optJSONObject("tags") ?: JSONObject())
}