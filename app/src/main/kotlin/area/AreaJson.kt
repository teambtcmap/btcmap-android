package area

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime

data class AreaJson(
    val id: Long,
    val tags: JSONObject?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun AreaJson.toArea(): Area {
    return Area(
        id = id,
        tags = tags!!,
        updatedAt = ZonedDateTime.parse(updatedAt),
    )
}

fun InputStream.toAreasJson(): List<AreaJson> {
    return toJsonArray().map {
        AreaJson(
            id = it.getLong("id"),
            tags = it.optJSONObject("tags") ?: JSONObject(),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.optString("deleted_at").ifBlank { null },
        )
    }
}