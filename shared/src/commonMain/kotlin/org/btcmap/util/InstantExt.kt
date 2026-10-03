package org.btcmap.util

import kotlin.time.Instant

/**
 * Parses [this] as an ISO-8601 instant, throwing when it is malformed.
 *
 * The API serialises timestamps with Rust's `time` crate, whose RFC 3339 form
 * omits the seconds when they are zero, so midnight arrives as
 * `2025-02-03T00:00Z`. `Instant.parse` rejects that as too short, so the zero
 * seconds are put back first; a value that already carries seconds (or
 * fractional seconds) is left for the strict parser.
 */
fun String.toInstant(): Instant = Instant.parse(withZeroSecondsIfOmitted())

/**
 * Parses [this] as an ISO-8601 instant, or null when it is malformed.
 *
 * Only display-only timestamps are optional like this; a malformed value must
 * not roll back a whole snapshot and leave the map empty, so it degrades to
 * null exactly like a missing field.
 */
fun String.toInstantOrNull(): Instant? = runCatching { toInstant() }.getOrNull()

/**
 * Matches an instant whose seconds are omitted: the time ends at `HH:MM` and
 * the zone follows immediately. A value that carries seconds or fractional
 * seconds does not match and is left for the strict parser.
 */
private val OMITTED_SECONDS = Regex("""^(.*?T\d{2}:\d{2})(Z|[+-]\d{2}:?\d{2})$""")

private fun String.withZeroSecondsIfOmitted(): String =
    OMITTED_SECONDS.replace(this) { "${it.groupValues[1]}:00${it.groupValues[2]}" }

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
