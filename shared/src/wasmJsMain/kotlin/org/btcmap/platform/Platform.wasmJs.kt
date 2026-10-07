package org.btcmap.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

actual fun currentLanguage(): String = "en"

actual fun currentLocale(): String = "en"

/**
 * Simple thousands grouping. The browser's `Intl.NumberFormat` is the proper
 * source; wiring it needs JS interop, so the web target starts with a neutral
 * fallback.
 */
actual fun formatInteger(value: Long): String {
    val digits = value.toString()
    val negative = digits.startsWith('-')
    val body = if (negative) digits.substring(1) else digits
    val grouped = body.reversed().chunked(3).joinToString(",").reversed()
    return if (negative) "-$grouped" else grouped
}

/**
 * The English weekday name. A localized name needs the browser's
 * `Intl.DateTimeFormat`; this is the neutral fallback until then.
 */
actual fun weekdayName(isoDayNumber: Int, language: String): String =
    DayOfWeek(isoDayNumber).name.lowercase().replaceFirstChar { it.titlecase() }
