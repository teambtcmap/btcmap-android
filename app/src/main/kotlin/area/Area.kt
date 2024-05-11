package area

import java.time.ZonedDateTime

data class Area(
    val id: Long,
    val tags: AreaTags,
    val updatedAt: ZonedDateTime,
)