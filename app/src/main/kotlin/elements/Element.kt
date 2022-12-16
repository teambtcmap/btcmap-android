package elements

import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

data class Element(
    val id: String,
    val lat: Double,
    val lon: Double,
    val osmJson: JsonObject,
    val tags: JsonObject,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
)
