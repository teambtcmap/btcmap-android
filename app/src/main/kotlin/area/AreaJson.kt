package area

import json.toJsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.InputStream
import java.time.ZonedDateTime

data class AreaJson(
    val id: Long,
    val tags: JsonObject?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun AreaJson.toArea(): Area {
    return Area(
        id = id,
        tags = tags!!,
        updatedAt = ZonedDateTime.parse(updatedAt),
    )
}

fun InputStream.toAreasJson(): List<AreaJson> {
    return toJsonArray().map {
        AreaJson(
            id = it.getLong("id"),
            tags = if (it.has("tags")) {
                Json.parseToJsonElement(it.getString("tags")).jsonObject
            } else {
                null
            },
            updatedAt = it.getString("updated_at"),
            deletedAt = it.optString("deleted_at").ifBlank { null },
        )
    }
}