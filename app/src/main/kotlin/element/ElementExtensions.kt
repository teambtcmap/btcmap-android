package element

import db.Element
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private val idToTags = mutableMapOf<String, JsonObject>()

fun Element.tags(): JsonObject {
    return idToTags.getOrPut(id) {
        val osmJson: JsonObject = Json.decodeFromString(osm_json)
        osmJson["tags"]!!.jsonObject
    }
}