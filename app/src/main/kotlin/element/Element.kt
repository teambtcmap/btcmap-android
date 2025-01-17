package element

import android.content.res.Resources
import org.json.JSONObject

data class Element(
    val id: Long,
    val overpassData: JSONObject,
    val tags: JSONObject,
    val updatedAt: String,
    val lat: Double,
    val lon: Double,
)

fun Element.osmTags(): OsmTags {
    return (overpassData.optJSONObject("tags") ?: JSONObject())
}

fun Element.osmTag(name: String): String {
    return osmTags().optString(name, "")
}

fun Element.name(res: Resources): String {
    return osmTags().name(res)
}