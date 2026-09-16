package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
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
    val website: HttpUrl,
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
        id = get("id").asLong,
        areaId = if (!has("area_id") || get("area_id").isJsonNull) null else get("area_id").asLong,
        lat = get("lat").asDouble,
        lon = get("lon").asDouble,
        name = get("name").asString,
        website = get("website").asString.toHttpUrl(),
        startsAt = ZonedDateTime.parse(get("starts_at").asString),
        endsAt = if (!has("ends_at") || get("ends_at").isJsonNull) null else ZonedDateTime.parse(
            get("ends_at").asString
        ),
    )
}

private fun InputStream.toGetEventsItems(): List<GetEventsItem> {
    return toJsonArray().map { it.toGetEventsItem() }
}
