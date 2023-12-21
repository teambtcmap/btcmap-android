package reports

import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime

data class Report(
    val areaId: String,
    val date: LocalDate,
    val tags: JSONObject,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
)
