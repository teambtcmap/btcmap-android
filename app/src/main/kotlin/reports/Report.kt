package reports

import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime

data class Report(
    val id: Long,
    val areaId: Long,
    val date: LocalDate,
    val tags: JSONObject,
    val updatedAt: ZonedDateTime,
)