package org.btcmap.util

import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

fun String.toZonedDateTime() = ZonedDateTime.parse(this)

/**
 * Parses a bundled timestamp, returning null when it is unparseable.
 *
 * Only display-only timestamps are optional like this; a malformed value must
 * not roll back a whole snapshot and leave the map empty, so it degrades to
 * null exactly like a missing field.
 */
fun String.toZonedDateTimeOrNull(): ZonedDateTime? =
    try {
        ZonedDateTime.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }

/**
 * Whether an event starting at this instant is still upcoming relative to
 * [now].
 *
 * This is the single definition of an upcoming event, shared by the map,
 * search and area screens; the database stats screen mirrors it in SQL. It is
 * strictly after [now], so an event whose start has passed is not upcoming.
 * The API represents an event with no start date as the epoch, which is simply
 * in the past and therefore not upcoming.
 */
fun ZonedDateTime.isUpcoming(now: ZonedDateTime): Boolean = isAfter(now)
