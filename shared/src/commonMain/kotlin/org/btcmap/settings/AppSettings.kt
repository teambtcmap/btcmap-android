package org.btcmap.settings

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The settings every host shares. They are plain [Settings] reads and writes, so
 * Android and desktop see the same values; the ones that need platform resources
 * (colors, style names) stay with their host.
 */

/** The map style the user picked; the platform decides what each one renders. */
enum class MapStyle {
    Auto,
    Liberty,
    Positron,
    Bright,
    Dark,
    DarkMatter,
}

var Settings.mapStyle: MapStyle
    get() = mapStyleFromPrefValue(getString(KEY_MAP_STYLE, "auto") ?: "auto")
    set(value) {
        putString(KEY_MAP_STYLE, value.toPrefValue())
    }

fun MapStyle.toPrefValue(): String = when (this) {
    MapStyle.Auto -> "auto"
    MapStyle.Liberty -> "liberty"
    MapStyle.Positron -> "positron"
    MapStyle.Bright -> "bright"
    MapStyle.Dark -> "dark"
    MapStyle.DarkMatter -> "dark_matter"
}

private fun mapStyleFromPrefValue(pref: String): MapStyle = when (pref) {
    "auto" -> MapStyle.Auto
    "liberty" -> MapStyle.Liberty
    "positron" -> MapStyle.Positron
    "bright" -> MapStyle.Bright
    "dark" -> MapStyle.Dark
    // "carto_dark_matter" is the value a previous build stored; the style was
    // rebased onto OpenFreeMap, so keep an existing choice on it.
    "dark_matter", "carto_dark_matter" -> MapStyle.DarkMatter
    else -> MapStyle.Auto
}

private const val KEY_MAP_STYLE = "mapStyle"

/**
 * The bundled style asset for [this] style, relative to the assets/resources
 * root. `Auto` follows [darkSystemTheme].
 */
fun MapStyle.bundledStyleAsset(darkSystemTheme: Boolean): String = when (this) {
    MapStyle.Auto -> if (darkSystemTheme) {
        "map-styles/dark-matter/style.json"
    } else {
        "map-styles/light/style.json"
    }

    MapStyle.Liberty -> "map-styles/liberty/style.json"
    MapStyle.Positron -> "map-styles/positron/style.json"
    MapStyle.Bright -> "map-styles/bright/style.json"
    MapStyle.Dark -> "map-styles/dark/style.json"
    MapStyle.DarkMatter -> "map-styles/dark-matter/style.json"
}

/**
 * The hosted style URL for [this] style, for a host without the bundled asset
 * styles. Android keeps using its assets; see `org.btcmap.settings.uri`.
 */
fun MapStyle.hostedStyleUrl(darkSystemTheme: Boolean): String = when (this) {
    MapStyle.Auto -> if (darkSystemTheme) HOSTED_DARK_STYLE_URL else HOSTED_LIGHT_STYLE_URL
    MapStyle.Dark, MapStyle.DarkMatter -> HOSTED_DARK_STYLE_URL
    MapStyle.Liberty -> HOSTED_LIBERTY_STYLE_URL
    MapStyle.Positron -> HOSTED_POSITRON_STYLE_URL
    MapStyle.Bright -> HOSTED_BRIGHT_STYLE_URL
}

private const val HOSTED_LIGHT_STYLE_URL = "https://static.btcmap.org/map-styles/light.json"
private const val HOSTED_DARK_STYLE_URL = "https://static.btcmap.org/map-styles/dark.json"
private const val HOSTED_LIBERTY_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val HOSTED_POSITRON_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
private const val HOSTED_BRIGHT_STYLE_URL = "https://tiles.openfreemap.org/styles/bright"

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
