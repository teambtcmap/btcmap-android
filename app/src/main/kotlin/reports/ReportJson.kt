package reports

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.ZonedDateTime

@Serializable
data class ReportJson(
    val areaId: String,
    val date: String,
    val tags: JsonObject,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String,
)

fun ReportJson.toReport(): Report {
    return Report(
        areaId = areaId,
        date = LocalDate.parse(date),
        tags = tags,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(updatedAt),
        deletedAt = if (deletedAt.isNotEmpty()) ZonedDateTime.parse(deletedAt) else null,
    )
}