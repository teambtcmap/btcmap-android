package org.btcmap.openinghours

/**
 * A contiguous opening span within a day, e.g. 08:00 to 17:00.
 *
 * [end] is null when the OpenStreetMap value only gives a start (`10:00+`),
 * in which case the span is rendered with a trailing plus instead of a range.
 */
data class TimeRange(val start: String, val end: String?) {

    /** Whether the span covers midnight to midnight, i.e. the whole day. */
    val isAllDay: Boolean
        get() = start == "00:00" && (end == "24:00" || end == null)

    /** The span as "08:00–17:00", or "10:00+" when there is no end. */
    fun format(): String = if (end == null) "$start+" else "$start–$end"
}
