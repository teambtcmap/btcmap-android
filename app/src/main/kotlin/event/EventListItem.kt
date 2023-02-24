package event

import java.time.ZonedDateTime

data class EventListItem(
    val eventType: String,
    val elementId: String,
    val elementName: String,
    val eventDate: ZonedDateTime,
    val userName: String,
    val userTips: String,
)