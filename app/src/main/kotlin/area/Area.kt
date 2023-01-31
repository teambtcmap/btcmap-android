package area

import java.time.ZonedDateTime

data class Area(
    val id: String,
    val tags: AreaTags,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
)