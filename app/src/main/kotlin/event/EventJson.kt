package event

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime

data class EventJson(
    val id: Long,
    val userId: Long?,
    val elementId: Long?,
    val type: Long?,
    val tags: JSONObject?,
    val createdAt: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun EventJson.toEvent(): Event {
    return Event(
        id = id,
        userId = userId!!,
        elementId = elementId!!,
        type = type!!,
        tags = tags!!,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(updatedAt),
    )
}

fun InputStream.toEventsJson(): List<EventJson> {
    return toJsonArray().map {
        val userId = it.optLong("user_id")
        val elementId = it.optLong("element_id")
        val type = it.optLong("type")

        EventJson(
            id = it.getLong("id"),
            userId = if (userId == 0L) {
                null
            } else userId,
            elementId = if (elementId == 0L) {
                null
            } else elementId,
            type = if (type == 0L) {
                null
            } else type,
            tags = it.optJSONObject("tags") ?: JSONObject(),
            createdAt = it.optString("created_at").ifBlank { null },
            updatedAt = it.getString("updated_at"),
            deletedAt = it.optString("deleted_at").ifBlank { null },
        )
    }
}