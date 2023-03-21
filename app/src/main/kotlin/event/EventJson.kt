package event

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class EventJson(
    val id: Long,
    val type: String,
    val elementId: String,
    val userId: Long,
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
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(updatedAt),
        deletedAt = if (deletedAt.isNotEmpty()) ZonedDateTime.parse(deletedAt) else null,
    )
}