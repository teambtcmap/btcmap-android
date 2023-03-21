package area

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

@Serializable
data class AreaJson(
    val id: String,
    val tags: JsonObject,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String,
)

fun AreaJson.valid(): Boolean {
    return tags.contains("name") && tags.contains("geo_json")
}

fun AreaJson.toArea(): Area {
    return Area(
        id = id,
        tags = tags,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(updatedAt),
        deletedAt = if (deletedAt.isNotEmpty()) ZonedDateTime.parse(deletedAt) else null,
    )
}