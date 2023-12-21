package reports

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate
import java.time.ZonedDateTime

data class ReportJson(
    val areaId: String,
    val date: String,
    val tags: JSONObject,
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

fun InputStream.toReportsJson(): List<ReportJson> {
    return toJsonArray().map {
        ReportJson(
            areaId = it.getString("area_id"),
            date = it.getString("date"),
            tags = it.getJSONObject("tags"),
            createdAt = it.getString("created_at"),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.getString("deleted_at"),
        )
    }
}