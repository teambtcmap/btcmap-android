package org.btcmap.settings

import io.ktor.http.Url
import org.btcmap.util.toUrl
import org.btcmap.util.toUrlOrNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

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
var Settings.apiUrl: Url
    get() = (getString(KEY_API_URL, null) ?: DEFAULT_API_URL)
        .toUrlOrNull()
        ?: DEFAULT_API_URL.toUrl()
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

private const val KEY_MAP_TILT_ENABLED = "map_tilt_enabled"

var Settings.mapTiltEnabled: Boolean
    get() = getBoolean(KEY_MAP_TILT_ENABLED, false)
    set(value) {
        putBoolean(KEY_MAP_TILT_ENABLED, value)
    }

/**
 * Whether [this] style draws a dark basemap. [MapStyle.Auto] follows the system
 * theme, so the caller tells us whether the system is in dark mode.
 */
fun MapStyle.isDark(darkSystemTheme: Boolean): Boolean = when (this) {
    MapStyle.Dark, MapStyle.DarkMatter -> true
    MapStyle.Auto -> darkSystemTheme
    MapStyle.Liberty, MapStyle.Positron, MapStyle.Bright -> false
}

private const val KEY_VERIFIED_FILTER_YEARS = "verified_filter_years"

/** The window, in years, inside which a place must have been verified to show. */
var Settings.verifiedFilterYears: Int
    get() = getInt(KEY_VERIFIED_FILTER_YEARS, 3)
    set(value) {
        putInt(KEY_VERIFIED_FILTER_YEARS, value)
    }

/**
 * The oldest verification instant the map still shows: places verified before
 * this moment are hidden. Derived from [Settings.verifiedFilterYears], so the
 * map, not just the settings screen, honours the choice.
 *
 * The result is normalised to UTC: the viewport query compares it with
 * `julianday()`, which returns NULL for a zone id such as `[Asia/Bangkok]`, so
 * a cutoff carrying one would hide every place instead of the old ones.
 */
fun Settings.verifiedFilterMinVerifiedAt(now: Instant = Clock.System.now()): Instant {
    return now.minus(DateTimePeriod(years = verifiedFilterYears), TimeZone.UTC)
}
