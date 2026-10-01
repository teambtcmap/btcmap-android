package org.btcmap.openinghours

/**
 * One weekday's opening times in a parsed [OpeningHours] value.
 */
sealed interface DaySchedule {

    /** The day is closed all day. */
    data object Closed : DaySchedule

    /** The day is open around the clock. */
    data object OpenAllDay : DaySchedule

    /** The day is open during [ranges], which may cross midnight. */
    data class Open(val ranges: List<TimeRange>) : DaySchedule
}
