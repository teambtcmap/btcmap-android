@file:Suppress("UNUSED_PARAMETER")

package org.btcmap.settings

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import org.btcmap.App
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
fun MapStyle.uri(context: Context): String {
    return "asset://" + bundledStyleAsset(darkSystemTheme = isNightMode(context))
}

fun MapStyle.offlineStyleUrl(context: Context): String =
    offlineDownloadStyleUrl(isNightMode(context))

private fun isNightMode(context: Context): Boolean {
    return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
}

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

fun Settings.buttonAccentColor(context: Context): Int = mapColor(MapColor.ButtonAccent)

fun Settings.setButtonAccentColor(color: Int?) = setMapColor(MapColor.ButtonAccent, color)
