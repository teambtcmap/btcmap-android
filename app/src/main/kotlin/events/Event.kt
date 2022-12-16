package events

import java.time.ZonedDateTime

data class Event(
    val id: Long,
    val type: String,
    val elementId: String,
    val userId: Long,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
)
