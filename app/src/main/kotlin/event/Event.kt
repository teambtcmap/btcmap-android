package event

import org.json.JSONObject
import java.time.ZonedDateTime

data class Event(
    val id: Long,
    val type: String,
    val elementId: String,
    val userId: Long,
    val tags: JSONObject,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
)
