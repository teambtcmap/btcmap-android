package elements

import kotlinx.serialization.json.JsonObject

data class AreaElement(
    val id: String,
    val icon: String,
    val osmTags: JsonObject,
)