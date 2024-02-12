package element

import org.json.JSONArray

data class AreaElement(
    val id: String,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val osmTags: OsmTags,
    val issues: JSONArray,
)