package org.btcmap.ui

import kotlin.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formats an instant with a java.time formatter.
 *
 * Temporary: localized date and time formatting still runs on java.time and
 * moves to kotlinx-datetime with the rest of the locale work.
 */
internal fun Instant.format(formatter: DateTimeFormatter): String =
    formatter.format(toJavaInstant())

/** The device-zone calendar date of this instant. */
internal fun Instant.toLocalDate(): java.time.LocalDate =
    toJavaInstant().atZone(ZoneId.systemDefault()).toLocalDate()

private fun Instant.toJavaInstant(): java.time.Instant =
    java.time.Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())
