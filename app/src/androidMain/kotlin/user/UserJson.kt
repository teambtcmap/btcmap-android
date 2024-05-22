package user

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime

data class UserJson(
    val id: Long,
    val osmData: JSONObject?,
    val tags: JSONObject?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun UserJson.toUser(): User {
    return User(
        id = id,
        osmData = osmData!!,
        tags = tags!!,
        updatedAt = ZonedDateTime.parse(updatedAt),
    )
}

fun InputStream.toUsersJson(): List<UserJson> {
    return toJsonArray().map {
        UserJson(
            id = it.getLong("id"),
            osmData = it.optJSONObject("osm_data") ?: JSONObject(),
            tags = it.optJSONObject("tags") ?: JSONObject(),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.optString("deleted_at").ifBlank { null },
        )
    }
}