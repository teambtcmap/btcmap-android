package org.btcmap.api

import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

/** The result of [submitEvent]: the id and review state of the new event. */
data class SubmitEventResponse(
    val id: Long,
    val status: String,
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

/**
 * Submits a new event as the authenticated user. Any signed-in user may call it;
 * the server gives the event an initial `pending` status unless the caller holds
 * an event manager, admin or root role, in which case it goes live immediately.
 *
 * [startsAt] and [endsAt] are floating local wall-clock times (ISO 8601 with no
 * offset, e.g. `2026-09-25T19:00:00`), and the request asks the server to infer
 * the event's zone from its coordinates with `timezone=auto`. That places the
 * chosen time in the zone of the event's location — not the caller's device — and
 * resolves the offset, including daylight saving, for that date. The two forms
 * the endpoint accepts (explicit offset vs. local + timezone) are mutually
 * exclusive, so no offset may be present in these values.
 */
suspend fun Api.submitEvent(
    lat: Double,
    lon: Double,
    name: String,
    website: String,
    startsAt: String,
    endsAt: String?,
): SubmitEventResponse {
    val url = buildUrl("v4", "events")

    val req = buildJsonObject {
        put("lat", lat)
        put("lon", lon)
        put("name", name)
        // Required by the endpoint, though an empty string is accepted.
        put("website", website)
        put("starts_at", startsAt)
        endsAt?.let { put("ends_at", it) }
        put("timezone", "auto")
    }

    return call(HttpMethod.Post, url, body = req) { body ->
        val response = body.toJsonObject()
        SubmitEventResponse(
            id = response.long("id"),
            status = response.string("status"),
        )
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
