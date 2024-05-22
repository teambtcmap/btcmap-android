package reports

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate
import java.time.ZonedDateTime

data class ReportJson(
    val id: Long,
    val areaId: Long?,
    val date: String?,
    val tags: JSONObject?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun ReportJson.toReport(): Report {
    return Report(
        id = id,
        areaId = areaId!!,
        date = LocalDate.parse(date),
        tags = tags!!,
        updatedAt = ZonedDateTime.parse(updatedAt),
    )
}

fun InputStream.toReportsJson(): List<ReportJson> {
    return toJsonArray().map {
        ReportJson(
            id = it.getLong("id"),
            areaId = it.optLong("area_id"),
            date = it.optString("date"),
            tags = it.optJSONObject("tags"),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.optString("deleted_at").ifBlank { null },
        )
    }
}