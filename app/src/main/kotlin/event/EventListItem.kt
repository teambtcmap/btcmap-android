package event

import org.json.JSONObject
import java.time.ZonedDateTime

data class EventListItem(
    val eventType: Long,
    val elementId: Long,
    val elementName: String,
    val eventDate: ZonedDateTime,
    val userName: String,
    val userTips: String,
    val tags: JSONObject,
)