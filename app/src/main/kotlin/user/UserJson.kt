package user

import json.toJsonArray
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime

data class UserJson(
    val id: Long,
    val osmJson: JSONObject,
    val tags: JSONObject,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String,
)

fun UserJson.toUser(): User {
    return User(
        id = id,
        osmJson = osmJson,
        tags = tags,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(updatedAt),
        deletedAt = if (deletedAt.isNotBlank()) ZonedDateTime.parse(deletedAt) else null,
    )
}

fun InputStream.toUsersJson(): List<UserJson> {
    return toJsonArray().map {
        UserJson(
            id = it.getLong("id"),
            osmJson = it.getJSONObject("osm_json"),
            tags = it.getJSONObject("tags"),
            createdAt = it.getString("created_at"),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.getString("deleted_at"),
        )
    }
}