package org.btcmap.comment

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

actual fun commentDateFormatter(timeZone: String, language: String): CommentDateFormatter {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.forLanguageTag(language))
        .withZone(ZoneId.of(timeZone))
    return CommentDateFormatter { instant ->
        formatter.format(
            java.time.Instant.ofEpochSecond(instant.epochSeconds, instant.nanosecondsOfSecond.toLong()),
        )
    }
}
