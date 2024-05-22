package event

import org.json.JSONObject
import java.time.ZonedDateTime

data class Event(
    val id: Long,
    val userId: Long,
    val elementId: Long,
    val type: Long,
    val tags: JSONObject,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
)
