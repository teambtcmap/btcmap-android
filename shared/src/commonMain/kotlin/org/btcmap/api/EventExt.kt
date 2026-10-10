package org.btcmap.api

import org.btcmap.db.table.event.Event
import org.btcmap.db.table.event.eventOf
import org.btcmap.util.toInstant

/**
 * The event row both the delta sync and the bundled seed store, so the two
 * paths map the API's fields the same way.
 */
internal fun GetEventsDeltaItem.toEvent(): Event = eventOf(
    id = id,
    lat = lat,
    lon = lon,
    name = name,
    website = website,
    startsAt = startsAt,
    endsAt = endsAt,
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)
