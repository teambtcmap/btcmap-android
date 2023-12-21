package user

import org.json.JSONObject
import java.time.ZonedDateTime

data class User(
    val id: Long,
    val osmJson: JSONObject,
    val tags: JSONObject,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
)
