package elementevents

data class ElementEvent(
    val date: String,
    val elementId: String,
    val elementLat: Double,
    val elementLon: Double,
    val elementName: String,
    val eventType: String,
    val user: String,
)