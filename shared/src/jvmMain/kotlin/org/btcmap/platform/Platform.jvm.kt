package org.btcmap.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual fun currentLanguage(): String = Locale.getDefault().language

actual fun formatInteger(value: Long): String = NumberFormat.getIntegerInstance().format(value)

actual fun weekdayName(isoDayNumber: Int, language: String): String =
    DayOfWeek.of(isoDayNumber).getDisplayName(TextStyle.FULL, Locale.forLanguageTag(language))
