package org.btcmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant

/**
 * An event's date and time text: [start] always, [end] only when the event
 * spans more than one day. A same-day range is folded into [start] by the
 * host's [dateRange] format, and an open-ended event has no [end].
 */
internal data class EventTimeText(val start: String, val end: String?)

/**
 * Formats an event's time the way every event surface shows it: one
 * "date, start - end" line for a same-day event, only the start's date and time
 * when the end is open, and two lines when it spans days. [dateRange] is the
 * host's localized same-day format, shared by the event, review and my-events
 * screens.
 */
@Composable
internal fun eventTimeText(
    startsAt: Instant,
    endsAt: Instant?,
    dateRange: (date: String, start: String, end: String) -> String,
): EventTimeText {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }

    return if (endsAt == null) {
        EventTimeText(startsAt.format(dateTimeFormatter), null)
    } else if (startsAt.toLocalDate() == endsAt.toLocalDate()) {
        EventTimeText(
            dateRange(
                startsAt.format(dateFormatter),
                startsAt.format(timeFormatter),
                endsAt.format(timeFormatter),
            ),
            null,
        )
    } else {
        EventTimeText(
            startsAt.format(dateTimeFormatter),
            endsAt.format(dateTimeFormatter),
        )
    }
}
