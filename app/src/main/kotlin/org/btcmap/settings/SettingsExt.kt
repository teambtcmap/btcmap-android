@file:Suppress("UNUSED_PARAMETER")

package org.btcmap.settings

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import org.btcmap.App
import org.btcmap.R
import org.btcmap.map.DEFAULT_MAP_CENTER_LAT
import org.btcmap.map.DEFAULT_MAP_CENTER_LON
import org.btcmap.map.DEFAULT_MAP_ZOOM

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

/**
 * Where the map was last left. The map screen saves these as the camera comes to
 * rest and reopens there, so the app returns to what the user was looking at.
 */
var Settings.mapCenterLat: Double
    get() = getFloat("mapCenterLat", DEFAULT_MAP_CENTER_LAT.toFloat()).toDouble()
    set(value) = putFloat("mapCenterLat", value.toFloat())

var Settings.mapCenterLon: Double
    get() = getFloat("mapCenterLon", DEFAULT_MAP_CENTER_LON.toFloat()).toDouble()
    set(value) = putFloat("mapCenterLon", value.toFloat())

var Settings.mapZoom: Double
    get() = getFloat("mapZoom", DEFAULT_MAP_ZOOM.toFloat()).toDouble()
    set(value) = putFloat("mapZoom", value.toFloat())

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
    return "asset://" + bundledStyleAsset(darkSystemTheme = isNightMode(context))
}

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

/*
 * The map colors and the verification window moved to `:shared` (see
 * `MapColors.kt` and `AppSettings.kt`) so the desktop app reads the same keys
 * and defaults. These wrappers keep the Android call sites unchanged; the
 * `context` is no longer consulted because the defaults do not vary by theme.
 */
fun Settings.markerBackgroundColor(context: Context): Int = mapColor(MapColor.MarkerBackground)

fun Settings.setMarkerBackgroundColor(color: Int?) = setMapColor(MapColor.MarkerBackground, color)

fun Settings.boostedMarkerBackgroundColor(): Int = mapColor(MapColor.BoostedMarkerBackground)

fun Settings.setBoostedMarkerBackgroundColor(color: Int?) =
    setMapColor(MapColor.BoostedMarkerBackground, color)

fun Settings.boostedMarkerIconColor(): Int = mapColor(MapColor.BoostedMarkerIcon)

fun Settings.setBoostedMarkerIconColor(color: Int?) = setMapColor(MapColor.BoostedMarkerIcon, color)

fun Settings.markerIconColor(context: Context): Int = mapColor(MapColor.MarkerIcon)

fun Settings.setMarkerIconColor(color: Int?) = setMapColor(MapColor.MarkerIcon, color)

fun Settings.badgeBackgroundColor(context: Context): Int = mapColor(MapColor.BadgeBackground)

fun Settings.setBadgeBackgroundColor(color: Int?) = setMapColor(MapColor.BadgeBackground, color)

fun Settings.badgeTextColor(context: Context): Int = mapColor(MapColor.BadgeText)

fun Settings.setBadgeTextColor(color: Int?) = setMapColor(MapColor.BadgeText, color)

fun Settings.buttonBackgroundColor(context: Context): Int = mapColor(MapColor.ButtonBackground)

fun Settings.setButtonBackgroundColor(color: Int?) = setMapColor(MapColor.ButtonBackground, color)

fun Settings.buttonIconColor(context: Context): Int = mapColor(MapColor.ButtonIcon)

fun Settings.setButtonIconColor(color: Int?) = setMapColor(MapColor.ButtonIcon, color)

fun Settings.buttonBorderColor(context: Context): Int = mapColor(MapColor.ButtonBorder)

fun Settings.setButtonBorderColor(color: Int?) = setMapColor(MapColor.ButtonBorder, color)

fun Int.toVerifiedFilterYears(context: Context): String {
    return when (this) {
        1 -> context.getString(R.string.verified_filter_1_year)
        2 -> context.getString(R.string.verified_filter_2_years)
        3 -> context.getString(R.string.verified_filter_3_years)
        else -> ""
    }
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
