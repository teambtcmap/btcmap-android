package org.btcmap.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatcher for blocking IO. Every target has one: on the JVM and Android
 * it is the dedicated IO pool, elsewhere the default dispatcher.
 */
expect val ioDispatcher: CoroutineDispatcher

/**
 * The language the user picked in the settings, as a BCP-47 tag, or null to
 * follow the device. [currentLanguage] and [currentLocale] read it, so both the
 * UI string catalog and the localized content follow the choice; a host sets it
 * from the stored `Settings.language` on startup and after a change.
 */
expect var languageOverride: String?

/** The language as a BCP-47 language tag, e.g. "en" or "de". */
expect fun currentLanguage(): String

/**
 * The locale as a BCP-47 tag including any region, e.g. "en", "pt-BR". UI
 * strings resolve against this so a regional variant can be picked.
 */
expect fun currentLocale(): String

/** Formats an integer with the device's grouping, e.g. "1,234". */
expect fun formatInteger(value: Long): String

/** The full localized name of an ISO weekday (1 = Monday, 7 = Sunday). */
expect fun weekdayName(isoDayNumber: Int, language: String): String
