package element

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
