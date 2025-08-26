package event

import okhttp3.HttpUrl
import java.time.ZonedDateTime

data class Event(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name : String,
    val website: HttpUrl,
    val starts_at: ZonedDateTime,
    val ends_at: ZonedDateTime?,
)
