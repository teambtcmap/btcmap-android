package bundle

import java.time.ZonedDateTime

data class BundledPlace(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String,
    val comments: Long?,
    val boostedUntil: ZonedDateTime?,
)
