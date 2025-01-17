package area

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

typealias AreaTags = JsonObject

fun AreaTags.name(): String {
    return this["name"]?.jsonPrimitive?.contentOrNull ?: ""
}