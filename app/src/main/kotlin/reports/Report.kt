package reports

import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.ZonedDateTime

data class Report(
    val areaId: String,
    val date: LocalDate,
    val tags: JsonObject,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
)
