package areas

import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

data class Area(
    val id: String,
    val tags: JsonObject,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
)
