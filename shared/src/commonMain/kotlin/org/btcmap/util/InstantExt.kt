package org.btcmap.util

import kotlin.time.Instant

/** Parses [this] as an ISO-8601 instant, throwing when it is malformed. */
fun String.toInstant(): Instant = Instant.parse(this)

/**
 * Parses [this] as an ISO-8601 instant, or null when it is malformed.
 *
 * Only display-only timestamps are optional like this; a malformed value must
 * not roll back a whole snapshot and leave the map empty, so it degrades to
 * null exactly like a missing field.
 */
fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()

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
fun Instant.isUpcoming(now: Instant): Boolean = this > now
