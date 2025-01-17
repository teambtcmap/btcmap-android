package area

import kotlinx.datetime.Instant

data class Area(
    val id: Long,
    val tags: AreaTags,
    val updatedAt: Instant,
)