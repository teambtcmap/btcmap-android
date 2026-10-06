package org.btcmap.settings

/**
 * The time windows the activity feed can show. Shared so the Compose root can
 * build the filter chips without the Android resource mapping; the labels live
 * with the host.
 */
enum class ActivityInterval(val days: Int) {
    Day(1),
    Week(7),
    Month(30),
    HalfYear(180),
    Year(365),
}

private const val KEY_ACTIVITY_INTERVAL_DAYS = "activity_interval_days"

/** The stored activity window, defaulting to half a year. */
var Settings.activityIntervalDays: Int
    get() = getInt(KEY_ACTIVITY_INTERVAL_DAYS, ActivityInterval.HalfYear.days)
    set(value) {
        putInt(KEY_ACTIVITY_INTERVAL_DAYS, value)
    }
