package org.btcmap.settings

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import org.btcmap.App
import org.btcmap.R
import org.maplibre.android.geometry.LatLngBounds
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.ZoneOffset
import java.time.ZonedDateTime

lateinit var prefs: Settings
    private set

fun init(app: App) {
    val legacyPrefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    prefs = Settings(
        dbProvider = { app.db },
        legacyValues = { legacyPrefs.all },
        clearLegacyValues = { keys ->
            legacyPrefs.edit { keys.forEach { remove(it) } }
        },
    )
}

var Settings.mapViewport: LatLngBounds
    get() {
        return LatLngBounds.from(
            latNorth = getFloat("latNorth", 12.116667f + 0.04f).toDouble(),
            lonEast = getFloat("lonEast", -68.93333f + 0.04f + 0.03f).toDouble(),
            latSouth = getFloat("latSouth", 12.116667f - 0.04f).toDouble(),
            lonWest = getFloat("lonWest", -68.93333f - 0.04f + 0.03f).toDouble(),
        )
    }
    set(value) {
        putFloat("latNorth", value.latitudeNorth.toFloat())
        putFloat("lonEast", value.longitudeEast.toFloat())
        putFloat("latSouth", value.latitudeSouth.toFloat())
        putFloat("lonWest", value.longitudeWest.toFloat())
    }

var Settings.mapStyle: MapStyle
    get() {
        return mapStyleFromPrefValue(getString("mapStyle", "auto") ?: "auto")
    }
    set(value) {
        putString("mapStyle", value.toPrefValue())
    }

enum class MapStyle {
    Auto,
    Liberty,
    Positron,
    Bright,
    Dark,
    DarkMatter,
}

private fun mapStyleFromPrefValue(pref: String): MapStyle {
    return when (pref) {
        "auto" -> MapStyle.Auto
        "liberty" -> MapStyle.Liberty
        "positron" -> MapStyle.Positron
        "bright" -> MapStyle.Bright
        "dark" -> MapStyle.Dark
        // "carto_dark_matter" is the value a previous build stored; the style
        // was rebased onto OpenFreeMap, so keep an existing choice on it.
        "dark_matter", "carto_dark_matter" -> MapStyle.DarkMatter
        else -> MapStyle.Auto
    }
}

fun MapStyle.toPrefValue(): String {
    return when (this) {
        MapStyle.Auto -> "auto"
        MapStyle.Liberty -> "liberty"
        MapStyle.Positron -> "positron"
        MapStyle.Bright -> "bright"
        MapStyle.Dark -> "dark"
        MapStyle.DarkMatter -> "dark_matter"
    }
}

fun MapStyle.name(context: Context): String {
    return when (this) {
        MapStyle.Auto -> context.getString(R.string.style_auto)
        MapStyle.Liberty -> context.getString(R.string.style_liberty)
        MapStyle.Positron -> context.getString(R.string.style_positron)
        MapStyle.Bright -> context.getString(R.string.style_bright)
        MapStyle.Dark -> context.getString(R.string.style_dark)
        MapStyle.DarkMatter -> context.getString(R.string.style_dark_matter)
    }
}

fun MapStyle.uri(context: Context): String {
    return when (this) {
        MapStyle.Auto -> {
            if (isNightMode(context)) {
                "asset://map-styles/dark-matter/style.json"
            } else {
                "asset://map-styles/light/style.json"
            }
        }

        MapStyle.Liberty -> "asset://map-styles/liberty/style.json"
        MapStyle.Positron -> "asset://map-styles/positron/style.json"
        MapStyle.Bright -> "asset://map-styles/bright/style.json"
        MapStyle.Dark -> "asset://map-styles/dark/style.json"
        MapStyle.DarkMatter -> "asset://map-styles/dark-matter/style.json"
    }
}

/**
 * The hosted style URL to hand to MapLibre's offline manager.
 *
 * Offline regions can only be created from an http(s) style: the native
 * downloader cannot resolve the bundled `asset://` styles. These URLs all draw
 * OpenFreeMap's tiles, the same ones the bundled styles draw, so a downloaded
 * region also serves the bundled style at render time. Dark Matter is rebased
 * onto OpenFreeMap, so it downloads through the Liberty style; the pack
 * therefore also serves whichever of the two was selected. Auto keeps the
 * light and dark hosted URLs so its light/dark variants stay one family, even
 * though its night variant renders the bundled Dark Matter.
 */
fun MapStyle.offlineStyleUrl(context: Context): String {
    return when (this) {
        MapStyle.Auto -> if (isNightMode(context)) {
            DARK_STYLE_URL
        } else {
            LIGHT_STYLE_URL
        }

        MapStyle.Liberty -> LIBERTY_STYLE_URL
        MapStyle.Positron -> POSITRON_STYLE_URL
        MapStyle.Bright -> BRIGHT_STYLE_URL
        MapStyle.Dark -> DARK_STYLE_URL
        MapStyle.DarkMatter -> LIBERTY_STYLE_URL
    }
}

private fun isNightMode(context: Context): Boolean {
    return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
}

/**
 * Groups the hosted offline style URLs that render the same map.
 *
 * [MapStyle.Auto] resolves to the light or dark hosted style depending on the
 * system theme, but both draw the same sources, so they share one family: a
 * pack downloaded at noon must not read as "a different style" once dark mode
 * turns on. [MapStyle.DarkMatter] downloads through the Liberty style (see
 * [offlineStyleUrl]) and so shares its family for the same reason. Every other
 * style keeps its own identity.
 */
internal fun offlineStyleFamily(styleUrl: String): String = when (styleUrl) {
    LIGHT_STYLE_URL, DARK_STYLE_URL -> AUTO_STYLE_FAMILY
    else -> styleUrl
}

private const val AUTO_STYLE_FAMILY = "auto"

private const val LIGHT_STYLE_URL = "https://static.btcmap.org/map-styles/light.json"
private const val DARK_STYLE_URL = "https://static.btcmap.org/map-styles/dark.json"
private const val LIBERTY_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val POSITRON_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
private const val BRIGHT_STYLE_URL = "https://tiles.openfreemap.org/styles/bright"

fun Settings.mapStyleIsDark(): Boolean {
    return when (mapStyle) {
        MapStyle.Dark, MapStyle.DarkMatter -> true
        MapStyle.Auto -> {
            false
        }

        else -> false
    }
}

fun Settings.markerBackgroundColor(context: Context): Int {
    val customColor = getIntOrNull("markerBackgroundColor")
    if (customColor != null) return customColor

    val isDark = mapStyleIsDark() ||
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFF0e95af.toInt() else 0xFF0e95af.toInt()
}

fun Settings.setMarkerBackgroundColor(color: Int?) {
    putInt("markerBackgroundColor", color)
}

private const val KEY_BOOSTED_MARKER_BACKGROUND_COLOR = "boostedMarkerBackgroundColor"

fun Settings.boostedMarkerBackgroundColor(): Int {
    return getInt(KEY_BOOSTED_MARKER_BACKGROUND_COLOR, "#f7931a".toColorInt())
}

fun Settings.setBoostedMarkerBackgroundColor(color: Int?) {
    putInt(KEY_BOOSTED_MARKER_BACKGROUND_COLOR, color)
}

private const val KEY_BOOSTED_MARKER_ICON_COLOR = "boostedMarkerIconColor"

fun Settings.boostedMarkerIconColor(): Int {
    return getInt(KEY_BOOSTED_MARKER_ICON_COLOR, 0xFFFFFFFF.toInt())
}

fun Settings.setBoostedMarkerIconColor(color: Int?) {
    putInt(KEY_BOOSTED_MARKER_ICON_COLOR, color)
}

fun Settings.markerIconColor(context: Context): Int {
    val customColor = getIntOrNull("markerIconColor")
    if (customColor != null) return customColor

    val isDark = mapStyleIsDark() ||
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt() // White for both
}

fun Settings.setMarkerIconColor(color: Int?) {
    putInt("markerIconColor", color)
}

fun Settings.badgeBackgroundColor(context: Context): Int {
    val customColor = getIntOrNull("badgeBackgroundColor")
    if (customColor != null) return customColor

    val isDark = mapStyleIsDark() ||
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFF00a63e.toInt() else 0xFF00a63e.toInt()
}

fun Settings.setBadgeBackgroundColor(color: Int?) {
    putInt("badgeBackgroundColor", color)
}

fun Settings.badgeTextColor(context: Context): Int {
    val customColor = getIntOrNull("badgeTextColor")
    if (customColor != null) return customColor

    val isDark = mapStyleIsDark() ||
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt()
}

fun Settings.setBadgeTextColor(color: Int?) {
    putInt("badgeTextColor", color)
}

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

private const val KEY_BUTTON_BACKGROUND_COLOR = "buttonBackgroundColor"

fun Settings.buttonBackgroundColor(context: Context): Int {
    val customColor = getIntOrNull(KEY_BUTTON_BACKGROUND_COLOR)
    if (customColor != null) return customColor

    val isDark = mapStyleIsDark() ||
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFF1f2937.toInt() else 0xFF1f2937.toInt()
}

fun Settings.setButtonBackgroundColor(color: Int?) {
    putInt(KEY_BUTTON_BACKGROUND_COLOR, color)
}

private const val KEY_BUTTON_ICON_COLOR = "buttonIconColor"

fun Settings.buttonIconColor(context: Context): Int {
    val customColor = getIntOrNull(KEY_BUTTON_ICON_COLOR)
    if (customColor != null) return customColor

    val isDark = mapStyleIsDark() ||
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt()
}

fun Settings.setButtonIconColor(color: Int?) {
    putInt(KEY_BUTTON_ICON_COLOR, color)
}

private const val KEY_BUTTON_BORDER_COLOR = "buttonBorderColor"

fun Settings.buttonBorderColor(context: Context): Int {
    val customColor = getIntOrNull(KEY_BUTTON_BORDER_COLOR)
    if (customColor != null) return customColor

    val isDark = mapStyleIsDark() ||
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt()
}

fun Settings.setButtonBorderColor(color: Int?) {
    putInt(KEY_BUTTON_BORDER_COLOR, color)
}

/**
 * The stored session token, or null when signed out. It is written only through
 * [Settings.replaceSession], [Settings.clearSession] and
 * [Settings.clearSessionIfTokenMatches], which keep it consistent with the
 * cached account and serialize against concurrent session changes. It is read
 * from the in-memory session cache and never touches the database, so it is safe
 * to call from the main thread.
 */
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

private const val KEY_VERIFIED_FILTER_YEARS = "verified_filter_years"

var Settings.verifiedFilterYears: Int
    get() {
        return getInt(KEY_VERIFIED_FILTER_YEARS, 3)
    }
    set(value) {
        putInt(KEY_VERIFIED_FILTER_YEARS, value)
    }

fun Int.toVerifiedFilterYears(context: Context): String {
    return when (this) {
        1 -> context.getString(R.string.verified_filter_1_year)
        2 -> context.getString(R.string.verified_filter_2_years)
        3 -> context.getString(R.string.verified_filter_3_years)
        else -> ""
    }
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
fun Settings.verifiedFilterMinVerifiedAt(now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
    return now.minusYears(verifiedFilterYears.toLong()).withZoneSameInstant(ZoneOffset.UTC)
}

enum class ActivityInterval(val days: Int) {
    Day(1),
    Week(7),
    Month(30),
    HalfYear(180),
    Year(365);

    fun name(context: Context): String = when (this) {
        Day -> context.getString(R.string.activity_interval_day)
        Week -> context.getString(R.string.activity_interval_week)
        Month -> context.getString(R.string.activity_interval_month)
        HalfYear -> context.getString(R.string.activity_interval_half_year)
        Year -> context.getString(R.string.activity_interval_year)
    }
}

private const val KEY_ACTIVITY_INTERVAL_DAYS = "activity_interval_days"

var Settings.activityIntervalDays: Int
    get() = getInt(KEY_ACTIVITY_INTERVAL_DAYS, ActivityInterval.HalfYear.days)
    set(value) {
        putInt(KEY_ACTIVITY_INTERVAL_DAYS, value)
    }
