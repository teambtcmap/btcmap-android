package org.btcmap.api

import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import org.btcmap.util.toInstant
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import org.btcmap.util.toUrlOrNull

data class GetEventsItem(
    val id: Long,
    // The v4 event payload carries no area association, so the event-to-area
    // link is resolved geometrically from the coordinates (see
    // `org.btcmap.db.table.event.isWithin`).
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: Url?,
    val startsAt: Instant,
    val endsAt: Instant?,
)

/**
 * A row of the incremental `GET /v4/events` change log. Unlike [GetEventsItem]
 * (used by the single-event endpoint, which omits them), a delta row always
 * carries [updatedAt] and may carry [deletedAt] as a tombstone.
 */
data class GetEventsDeltaItem(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: Url?,
    val startsAt: Instant,
    val endsAt: Instant?,
    val updatedAt: String,
    val deletedAt: String?,
)

suspend fun Api.getEvents(updatedSince: Instant, limit: Long): List<GetEventsDeltaItem> {
    val url = buildUrl("v4", "events") {
        // Always send updated_since: without it the endpoint falls back to the
        // legacy full snapshot, which omits updated_at and so cannot seed a
        // cursor for the next sync.
        addUpdatedSince(updatedSince)
        parameters.append("limit", "$limit")
        parameters.append("include_deleted", "true")
    }

    return call(HttpMethod.Get, url, withoutAuth = true) { it.toGetEventsDeltaItems() }
}

suspend fun Api.getEvent(id: Long): GetEventsItem {
    val url = buildUrl("v4", "events", "$id")

    return call(HttpMethod.Get, url, withoutAuth = true) { body ->
        body.toJsonObject().toGetEventsItem()
    }
}

internal fun JsonObject.toGetEventsItem(): GetEventsItem {
    return GetEventsItem(
        id = long("id"),
        lat = double("lat"),
        lon = double("lon"),
        name = string("name"),
        website = nonBlankStringOrNull("website")?.toUrlOrNull(),
        startsAt = string("starts_at").toApiInstant("starts_at"),
        endsAt = stringOrNull("ends_at")?.toApiInstant("ends_at"),
    )
}

private fun JsonObject.toGetEventsDeltaItem(): GetEventsDeltaItem {
    val item = toGetEventsItem()
    return GetEventsDeltaItem(
        id = item.id,
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

private fun String.toApiInstant(field: String): Instant {
    return try {
        toInstant()
    } catch (e: RuntimeException) {
        throw ApiParseException("Field '$field' is not a valid ISO 8601 datetime", e)
    }
}

private fun String.toGetEventsDeltaItems(): List<GetEventsDeltaItem> {
    return toJsonArray().map { it.toGetEventsDeltaItem() }
}
