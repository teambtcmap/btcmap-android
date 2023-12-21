package event

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime

data class EventJson(
    val id: Long,
    val type: String,
    val elementId: String,
    val userId: Long,
    val tags: JSONObject,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String,
)

fun EventJson.toEvent(): Event {
    return Event(
        id = id,
        type = type,
        elementId = elementId,
        userId = userId,
        tags = tags,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(updatedAt),
        deletedAt = if (deletedAt.isNotEmpty()) ZonedDateTime.parse(deletedAt) else null,
    )
}

fun InputStream.toEventsJson(): List<EventJson> {
    return toJsonArray().map {
        EventJson(
            id = it.getLong("id"),
            type = it.getString("type"),
            elementId = it.getString("element_id"),
            userId = it.getLong("user_id"),
            tags = it.getJSONObject("tags"),
            createdAt = it.getString("created_at"),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.getString("deleted_at"),
        )
    }
}