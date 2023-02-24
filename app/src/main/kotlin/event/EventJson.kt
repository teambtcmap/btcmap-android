package event

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class EventJson(
    val id: Long,
    val type: String,
    val element_id: String,
    val user_id: Long,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String,
)

fun EventJson.toEvent(): Event {
    return Event(
        id = id,
        type = type,
        elementId = element_id,
        userId = user_id,
        createdAt = ZonedDateTime.parse(created_at),
        updatedAt = ZonedDateTime.parse(updated_at),
        deletedAt = if (deleted_at.isNotEmpty()) ZonedDateTime.parse(deleted_at) else null,
    )
}