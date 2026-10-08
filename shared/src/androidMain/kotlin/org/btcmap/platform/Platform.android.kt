package org.btcmap.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

@Volatile
actual var languageOverride: String? = null

actual fun currentLanguage(): String =
    languageOverride?.substringBefore('-') ?: Locale.getDefault().language

actual fun currentLocale(): String =
    languageOverride ?: Locale.getDefault().toLanguageTag()

actual fun formatInteger(value: Long): String =
    NumberFormat.getIntegerInstance(localeForFormatting()).format(value)

private fun localeForFormatting(): Locale =
    languageOverride?.let(Locale::forLanguageTag) ?: Locale.getDefault()

actual fun weekdayName(isoDayNumber: Int, language: String): String =
    DayOfWeek.of(isoDayNumber).getDisplayName(TextStyle.FULL, Locale.forLanguageTag(language))
