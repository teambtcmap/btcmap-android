package org.btcmap.api

import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.btcmap.json.parseJson
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

/**
 * An event with its review status, as the review-aware read endpoints return it:
 * `GET /v4/users/me/events` (the caller's own submissions, any status) and
 * `GET /v4/events?status=pending` (the moderator queue). Unlike [GetEventsItem],
 * it carries the review [status].
 *
 * [startsAtLocal] and [endsAtLocal] are the event's own wall-clock times, with
 * their UTC offset dropped, so a client can re-submit them as floating local
 * times (the API is asked to re-infer the zone from the coordinates). This is
 * what makes "repeat this event" land on the same local hour whatever zone the
 * device is in.
 */
data class EventWithStatus(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: Url?,
    val status: String,
    val startsAt: Instant,
    val endsAt: Instant?,
    val startsAtLocal: String,
    val endsAtLocal: String?,
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
 * Lists every non-deleted event submitted by the authenticated user, newest
 * first, across all review states and including events that already started.
 * Requires the stored session token.
 */
suspend fun Api.getMyEvents(): List<EventWithStatus> {
    val url = buildUrl("v4", "users", "me", "events")

    return call(HttpMethod.Get, url) { body ->
        body.toJsonArray().map { it.toEventWithStatus() }
    }
}

/**
 * Lists the events awaiting review (`GET /v4/events?status=pending`): the
 * moderator queue. The server only returns events with a future start date, so
 * this is what the review screen and its count badge show. Requires a token with
 * an event manager, admin or root role; a regular user gets an empty list.
 */
suspend fun Api.getPendingEvents(): List<EventWithStatus> {
    val url = buildUrl("v4", "events") {
        parameters.append("status", "pending")
    }

    return call(HttpMethod.Get, url) { body ->
        body.toJsonArray().map { it.toEventWithStatus() }
    }
}

/**
 * Approves or rejects a submitted event (`PUT /v4/events/{id}/status`). Only
 * `live` and `rejected` are accepted; the event's location must also fall inside
 * the caller's geofence. Requires an event manager, admin or root role.
 */
suspend fun Api.setEventStatus(id: Long, status: String) {
    val url = buildUrl("v4", "events", "$id", "status")

    val req = buildJsonObject {
        put("status", status)
    }

    call(HttpMethod.Put, url, body = req) { }
}

/**
 * Revokes one of the authenticated user's own submissions while it is still
 * `pending` (`DELETE /v4/events/{id}`). The server soft-deletes the event, so it
 * disappears from [getMyEvents]; revoking an already-revoked event is a no-op.
 * Only the submitter may revoke, and only a pending event — a live event must be
 * taken down by a manager, so anything else is refused with `403`.
 */
suspend fun Api.revokeEvent(id: Long) {
    val url = buildUrl("v4", "events", "$id")

    call(HttpMethod.Delete, url) { }
}

/**
 * Soft-deletes any event (`POST /rpc`, method `delete_event`). This is the
 * privileged path: it requires an event manager, admin or root role, and the
 * event must fall inside the caller's geofence when one is set. The submitter's
 * self-service path for their own pending event is [revokeEvent].
 */
suspend fun Api.deleteEvent(id: Long) {
    val url = buildUrl("rpc")

    val body = buildJsonObject {
        put("jsonrpc", "2.0")
        put("method", "delete_event")
        putJsonObject("params") { put("id", id) }
        put("id", 1)
    }

    call(HttpMethod.Post, url, body = body) { response -> response.throwIfRpcError() }
}

/** JSON-RPC reports a failure under `error`, even on a 200 response. */
private fun String.throwIfRpcError() {
    val root = parseJson(this).jsonObject
    val error = root["error"] ?: return
    val message = runCatching {
        error.jsonObject["message"]?.jsonPrimitive?.content
    }.getOrNull()
    throw ApiException(code = 200, message = message ?: "RPC request failed")
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

private fun JsonObject.toEventWithStatus(): EventWithStatus {
    val item = toGetEventsItem()
    return EventWithStatus(
        id = item.id,
        lat = item.lat,
        lon = item.lon,
        name = item.name,
        website = item.website,
        status = string("status"),
        startsAt = item.startsAt,
        endsAt = item.endsAt,
        startsAtLocal = string("starts_at").toFloatingLocalTime(),
        endsAtLocal = stringOrNull("ends_at")?.toFloatingLocalTime(),
    )
}

/**
 * The wall-clock part of an RFC 3339 timestamp, dropping its UTC offset:
 * `2026-10-20T13:30:00+07:00` becomes `2026-10-20T13:30:00`. The offset follows
 * the time, so the first `+`, `-` or `Z` after the `T` marks where it starts.
 */
private fun String.toFloatingLocalTime(): String {
    val t = indexOf('T')
    if (t < 0) return this
    val offsetInRest = substring(t + 1).indexOfFirst { it == '+' || it == '-' || it == 'Z' }
    return if (offsetInRest >= 0) substring(0, t + 1 + offsetInRest) else this
}

internal fun JsonObject.toGetEventsDeltaItem(): GetEventsDeltaItem {
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
