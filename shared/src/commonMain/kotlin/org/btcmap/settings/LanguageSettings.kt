package org.btcmap.settings

import org.btcmap.platform.languageOverride

private const val KEY_LANGUAGE = "language"

/**
 * The UI language the user picked, as a BCP-47 tag, or null to follow the
 * device. The value is only persisted here; [applyLanguage] pushes it to the
 * platform, which is what makes the string catalog and the localized content
 * resolve against it.
 */
var Settings.language: String?
    get() = getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() }
    set(value) {
        putString(KEY_LANGUAGE, value)
    }

/**
 * Applies the stored [Settings.language] to [languageOverride]. Called by a host
 * on startup, and again after the setting changes, so the process-wide language
 * follows the user's choice.
 */
fun Settings.applyLanguage() {
    languageOverride = language
}
