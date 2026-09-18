package org.btcmap.map

import org.btcmap.db.table.event.Event
import java.time.Instant
import java.time.ZonedDateTime

/**
 * The delta sync stores the whole event change log, including events whose
 * start has already passed. The map and the local search only show upcoming
 * events, matching the old full-snapshot behavior.
 *
 * Events without a start date are serialized by the API as the epoch sentinel
 * and stay visible, treated as open-ended.
 */
internal fun Event.isUpcomingOrUndated(now: ZonedDateTime): Boolean {
    return startsAt.isAfter(now) || startsAt.toInstant() == Instant.EPOCH
}
