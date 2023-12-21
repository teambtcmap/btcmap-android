package element

import java.time.ZonedDateTime

data class ElementsCluster(
    val count: Long,
    val id: Long,
    val lat: Double,
    val lon: Double,
    val iconId: String,
    val boostExpires: ZonedDateTime?,
)