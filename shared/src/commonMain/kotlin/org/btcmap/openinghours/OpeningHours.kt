package org.btcmap.openinghours

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.btcmap.platform.currentLanguage
import org.btcmap.platform.weekdayName

/**
 * A week's opening hours parsed from an OpenStreetMap `opening_hours` value.
 *
 * [days] holds all seven days so callers can always render a complete week.
 */
data class OpeningHours(val days: Map<DayOfWeek, DaySchedule>) {

    /** Whether every day is open around the clock. */
    val isOpenAroundTheClock: Boolean
        get() = days.size == DayOfWeek.entries.size &&
            days.values.all { it is DaySchedule.OpenAllDay }

    /** Whether the place never opens. */
    val isAlwaysClosed: Boolean
        get() = days.isNotEmpty() && days.values.all { it is DaySchedule.Closed }

    /**
     * The zero-based index of [today]'s line in [toDisplayString], or null
     * when the whole week collapses to a single line (always open or closed).
     */
    fun todayLineIndex(today: DayOfWeek): Int? {
        if (isOpenAroundTheClock || isAlwaysClosed) return null
        return today.isoDayNumber - 1
    }

    /**
     * Renders the week as one line per weekday, e.g.:
     *
     *     Monday: 08:00–17:00
     *     ...
     *     Sunday: Closed
     *
     * A week that never closes collapses to [aroundTheClockLabel]; one that
     * never opens collapses to [closedLabel].
     */
    fun toDisplayString(
        closedLabel: String = "Closed",
        aroundTheClockLabel: String = "Open 24/7",
        language: String = currentLanguage(),
    ): String {
        if (isOpenAroundTheClock) return aroundTheClockLabel
        if (isAlwaysClosed) return closedLabel

        return DayOfWeek.entries.joinToString("\n") { day ->
            val name = weekdayName(day.isoDayNumber, language)
                .replaceFirstChar { it.titlecase() }
            "$name: ${days[day]?.describe(closedLabel) ?: closedLabel}"
        }
    }
}

private fun DaySchedule.describe(closedLabel: String): String = when (this) {
    DaySchedule.Closed -> closedLabel
    DaySchedule.OpenAllDay -> "00:00–24:00"
    is DaySchedule.Open -> ranges.joinToString(", ") { it.format() }
}
