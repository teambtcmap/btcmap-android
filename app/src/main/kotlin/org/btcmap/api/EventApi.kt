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

/**
 * A row of the incremental `GET /v4/events` change log. Unlike [GetEventsItem]
 * (used by the single and per-area endpoints, which omit them), a delta row
 * always carries [updatedAt] and may carry [deletedAt] as a tombstone.
 */
data class GetEventsDeltaItem(
    val id: Long,
    val areaId: Long?,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: HttpUrl?,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
    val updatedAt: String,
    val deletedAt: String?,
)

suspend fun Api.getEvents(updatedSince: ZonedDateTime, limit: Long): List<GetEventsDeltaItem> {
    val url = buildUrl("v4", "events") {
        // Always send updated_since: without it the endpoint falls back to the
        // legacy full snapshot, which omits updated_at and so cannot seed a
        // cursor for the next sync.
        addUpdatedSince(updatedSince)
        addQueryParameter("limit", "$limit")
        addQueryParameter("include_deleted", "true")
    }

    return call(Request.Builder().withoutAuth().url(url).build()) { it.toGetEventsDeltaItems() }
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

private fun JsonObject.toGetEventsDeltaItem(): GetEventsDeltaItem {
    val item = toGetEventsItem()
    return GetEventsDeltaItem(
        id = item.id,
        areaId = item.areaId,
        lat = item.lat,
        lon = item.lon,
        name = item.name,
        website = item.website,
        startsAt = item.startsAt,
        endsAt = item.endsAt,
        updatedAt = string("updated_at"),
        deletedAt = nonBlankStringOrNull("deleted_at"),
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

private fun InputStream.toGetEventsDeltaItems(): List<GetEventsDeltaItem> {
    return toJsonArray().map { it.toGetEventsDeltaItem() }
}
