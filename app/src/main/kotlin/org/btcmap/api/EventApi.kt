package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import java.io.InputStream
import java.time.ZonedDateTime

data class GetEventsItem(
    val id: Long,
    val areaId: Long?,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: HttpUrl?,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
)

suspend fun Api.getEvents(): List<GetEventsItem> {
    val url = url.newBuilder().addPathSegments("v4/events").build()

    return call(Request.Builder().url(url).build()) { it.toGetEventsItems() }
}

suspend fun Api.getEvent(id: Long): GetEventsItem {
    val url = url.newBuilder().addPathSegments("v4/events/$id").build()

    return call(Request.Builder().url(url).build()) { it.toJsonObject().toGetEventsItem() }
}

suspend fun Api.getAreaEvents(idOrAlias: String): List<GetEventsItem> {
    val url = url.newBuilder().addPathSegments("v4/areas/$idOrAlias/events").build()

    return call(Request.Builder().url(url).build()) { it.toGetEventsItems() }
}

internal fun JsonObject.toGetEventsItem(): GetEventsItem {
    return GetEventsItem(
        id = long("id"),
        areaId = longOrNull("area_id"),
        lat = double("lat"),
        lon = double("lon"),
        name = string("name"),
        website = nonBlankStringOrNull("website")?.toHttpUrlOrNull(),
        startsAt = ZonedDateTime.parse(string("starts_at")),
        endsAt = stringOrNull("ends_at")?.let { ZonedDateTime.parse(it) },
    )
}

private fun InputStream.toGetEventsItems(): List<GetEventsItem> {
    return toJsonArray().map { it.toGetEventsItem() }
}
