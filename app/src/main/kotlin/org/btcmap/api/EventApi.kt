package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.btcmap.auth.withoutAuth
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

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
    val url = buildUrl("v4", "events")

    return call(Request.Builder().withoutAuth().url(url).build()) { it.toGetEventsItems() }
}

suspend fun Api.getEvent(id: Long): GetEventsItem {
    val url = buildUrl("v4", "events", "$id")

    return call(Request.Builder().withoutAuth().url(url).build()) { it.toJsonObject().toGetEventsItem() }
}

suspend fun Api.getAreaEvents(idOrAlias: String): List<GetEventsItem> {
    val url = buildUrl("v4", "areas", idOrAlias, "events")

    return call(Request.Builder().withoutAuth().url(url).build()) { it.toGetEventsItems() }
}

internal fun JsonObject.toGetEventsItem(): GetEventsItem {
    return GetEventsItem(
        id = long("id"),
        areaId = longOrNull("area_id"),
        lat = double("lat"),
        lon = double("lon"),
        name = string("name"),
        website = nonBlankStringOrNull("website")?.toHttpUrlOrNull(),
        startsAt = string("starts_at").toApiZonedDateTime("starts_at"),
        endsAt = stringOrNull("ends_at")?.toApiZonedDateTime("ends_at"),
    )
}

private fun String.toApiZonedDateTime(field: String): ZonedDateTime {
    return try {
        ZonedDateTime.parse(this)
    } catch (e: DateTimeParseException) {
        throw ApiParseException("Field '$field' is not a valid ISO 8601 datetime", e)
    }
}

private fun InputStream.toGetEventsItems(): List<GetEventsItem> {
    return toJsonArray().map { it.toGetEventsItem() }
}
