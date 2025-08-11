package area

import kotlin.time.Instant

data class Area(
    val id: Long,
    val tags: AreaTags,
    val updatedAt: Instant,
)