package org.btcmap.util

import java.time.ZoneOffset
import java.time.ZonedDateTime

fun now(zone: ZoneOffset = ZoneOffset.UTC) = ZonedDateTime.now(zone)

fun String.toZonedDateTime() = ZonedDateTime.parse(this)

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