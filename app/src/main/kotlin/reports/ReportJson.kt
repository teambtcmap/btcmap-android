package reports

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.ZonedDateTime

@Serializable
data class ReportJson(
    val area_id: String,
    val date: String,
    val tags: JsonObject,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String,
)

fun ReportJson.toReport(): Report {
    return Report(
        areaId = area_id,
        date = LocalDate.parse(date),
        tags = tags,
        createdAt = ZonedDateTime.parse(created_at),
        updatedAt = ZonedDateTime.parse(updated_at),
        deletedAt = if (deleted_at.isNotEmpty()) ZonedDateTime.parse(deleted_at) else null,
    )
}