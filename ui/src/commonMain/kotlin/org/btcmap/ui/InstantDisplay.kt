package org.btcmap.ui

import kotlin.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.btcmap.platform.currentLocale

/**
 * Formats an instant with a java.time formatter, in the app's chosen language
 * rather than the device's: an English app on a Russian device must not show a
 * Russian date. [currentLocale] carries the user's language override, or the
 * device locale when none is set.
 *
 * Temporary: localized date and time formatting still runs on java.time and
 * moves to kotlinx-datetime with the rest of the locale work.
 */
internal fun Instant.format(formatter: DateTimeFormatter): String =
    formatter.withAppLocale().format(toJavaInstant().atZone(ZoneId.systemDefault()))

/** Recases [this] formatter to the app's chosen language (see [Instant.format]). */
internal fun DateTimeFormatter.withAppLocale(): DateTimeFormatter =
    withLocale(Locale.forLanguageTag(currentLocale()))

/** The device-zone calendar date of this instant. */
internal fun Instant.toLocalDate(): java.time.LocalDate =
    toJavaInstant().atZone(ZoneId.systemDefault()).toLocalDate()

private fun Instant.toJavaInstant(): java.time.Instant =
    java.time.Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())
