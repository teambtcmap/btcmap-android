package area

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime

data class AreaJson(
    val id: String,
    val tags: JSONObject,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String,
)

fun AreaJson.toArea(): Area {
    return Area(
        id = id,
        tags = tags,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(updatedAt),
        deletedAt = if (deletedAt.isNotEmpty()) ZonedDateTime.parse(deletedAt) else null,
    )
}

fun InputStream.toAreasJson(): List<AreaJson> {
    return toJsonArray().map {
        AreaJson(
            id = it.getString("id"),
            tags = it.getJSONObject("tags"),
            createdAt = it.getString("created_at"),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.getString("deleted_at"),
        )
    }
}