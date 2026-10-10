package org.btcmap.db.table.event

import io.ktor.http.Url
import kotlin.time.Instant

/**
 * The one place an [Event] is built: the single-event API read, the delta sync
 * and the bundled snapshot all map through it.
 *
 * [updatedAt]/[deletedAt] default for the single-event path, whose payload
 * carries neither; they only matter to the sync cursor and tombstones. Dates
 * are passed already parsed: the paths parse them with different strictness and
 * validate required fields themselves.
 */
internal fun eventOf(
    id: Long,
    lat: Double,
    lon: Double,
    name: String,
    website: Url?,
    startsAt: Instant,
    endsAt: Instant?,
    updatedAt: Instant = Instant.fromEpochSeconds(0),
    deletedAt: Instant? = null,
): Event = Event(
    id = id,
    lat = lat,
    lon = lon,
    name = name,
    website = website,
    startsAt = startsAt,
    endsAt = endsAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
