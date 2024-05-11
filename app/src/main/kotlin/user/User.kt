package user

import org.json.JSONObject
import java.time.ZonedDateTime

data class User(
    val id: Long,
    val osmData: JSONObject,
    val tags: JSONObject,
    val updatedAt: ZonedDateTime,
)
