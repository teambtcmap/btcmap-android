package org.btcmap.comment

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * An ISO date. The browser's `Intl.DateTimeFormat` is the proper localized
 * source; wiring it needs JS interop, so the web target starts with a neutral
 * fallback that still applies the requested time zone.
 */
actual fun commentDateFormatter(timeZone: String, language: String): CommentDateFormatter =
    CommentDateFormatter { instant ->
        val zone = runCatching { TimeZone.of(timeZone) }.getOrDefault(TimeZone.currentSystemDefault())
        instant.toLocalDateTime(zone).date.toString()
    }
