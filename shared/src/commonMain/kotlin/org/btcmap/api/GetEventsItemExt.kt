package org.btcmap.api

import org.btcmap.db.table.event.Event
import org.btcmap.db.table.event.eventOf

/**
 * The event screen's model, built from an API event. The API payload carries no
 * `updated_at`/`deleted_at`, so the defaults stand; they are only used for sync.
 */
fun GetEventsItem.toEvent(): Event = eventOf(
    id = id,
    lat = lat,
    lon = lon,
    name = name,
    website = website,
    startsAt = startsAt,
    endsAt = endsAt,
)
