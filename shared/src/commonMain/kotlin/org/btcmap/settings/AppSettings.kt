package org.btcmap.settings

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The settings every host shares. They are plain [Settings] reads and writes, so
 * Android and desktop see the same values; the ones that need platform resources
 * (colors, style names) stay with their host.
 */

private const val KEY_API_URL = "apiUrl"

private const val DEFAULT_API_URL = "https://api.btcmap.org"

/**
 * The configured API base URL. A stored value that cannot be parsed falls back
 * to the public API instead of throwing, so a malformed preference cannot take
 * down every request. `TokenSettingInterceptor` therefore never has to guess.
 */
var Settings.apiUrl: HttpUrl
    get() = (getString(KEY_API_URL, null) ?: DEFAULT_API_URL)
        .toHttpUrlOrNull()
        ?: DEFAULT_API_URL.toHttpUrl()
    set(value) {
        putString(KEY_API_URL, value.toString())
    }

val Settings.authToken: String?
    get() = sessionToken

val Settings.authorized: Boolean
    get() = !authToken.isNullOrBlank()

private const val KEY_SHOW_ATTRIBUTION = "show_attribution"

var Settings.showAttribution: Boolean
    get() = getBoolean(KEY_SHOW_ATTRIBUTION, true)
    set(value) {
        putBoolean(KEY_SHOW_ATTRIBUTION, value)
    }

private const val KEY_MAP_ROTATION_ENABLED = "map_rotation_enabled"

var Settings.mapRotationEnabled: Boolean
    get() = getBoolean(KEY_MAP_ROTATION_ENABLED, false)
    set(value) {
        putBoolean(KEY_MAP_ROTATION_ENABLED, value)
    }
