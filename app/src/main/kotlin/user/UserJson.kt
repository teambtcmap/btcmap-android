package user

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

@Serializable
data class UserJson(
    val id: Long,
    val osmJson: JsonObject,
    val tags: JsonObject,
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