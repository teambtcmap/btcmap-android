package area

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AreaJson(
    val id: String,
    val tags: JsonObject,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String,
)

fun AreaJson.valid(): Boolean {
    return tags.contains("name") && tags.contains("geo_json")
}