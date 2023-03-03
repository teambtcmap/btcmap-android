package user

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

@Serializable
data class UserJson(
    val id: Long,
    val osm_json: JsonObject,
    val tags: JsonObject,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String,
)

fun UserJson.toUser(): User {
    return User(
        id = id,
        osmJson = osm_json,
        tags = tags,
        createdAt = ZonedDateTime.parse(created_at),
        updatedAt = ZonedDateTime.parse(updated_at),
        deletedAt = if (deleted_at.isNotBlank()) ZonedDateTime.parse(deleted_at) else null,
    )
}