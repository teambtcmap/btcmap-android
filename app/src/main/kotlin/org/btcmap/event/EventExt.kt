package org.btcmap.event

import java.time.LocalDateTime
import org.btcmap.api.GetEventsItem
import org.btcmap.db.table.event.Event

/**
 * The event screen's model, built from an API event. The API payload carries no
 * `updated_at`/`deleted_at`, so the defaults stand; they are only used for sync.
 */
fun GetEventsItem.toEvent(): Event = Event(
    id = id,
    lat = lat,
    lon = lon,
    name = name,
    website = website,
    startsAt = startsAt,
    endsAt = endsAt,
)

/** Parses the floating local date-time a duplicate pre-fill carries, or null. */
fun String.toLocalDateTimeOrNull(): LocalDateTime? =
    runCatching { LocalDateTime.parse(this) }.getOrNull()
