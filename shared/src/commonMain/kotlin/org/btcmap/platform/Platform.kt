package org.btcmap.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatcher for blocking IO. Every target has one: on the JVM and Android
 * it is the dedicated IO pool, elsewhere the default dispatcher.
 */
expect val ioDispatcher: CoroutineDispatcher

/** The device language as a BCP-47 language tag, e.g. "en" or "de". */
expect fun currentLanguage(): String

/** Formats an integer with the device's grouping, e.g. "1,234". */
expect fun formatInteger(value: Long): String

/** The full localized name of an ISO weekday (1 = Monday, 7 = Sunday). */
expect fun weekdayName(isoDayNumber: Int, language: String): String
