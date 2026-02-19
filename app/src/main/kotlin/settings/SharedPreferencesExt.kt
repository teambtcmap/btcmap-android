package settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.content.edit
import org.btcmap.R
import org.maplibre.android.geometry.LatLngBounds
import androidx.core.graphics.toColorInt
import app.App
import map.getOnPrimaryContainerColor
import map.getOnTertiaryContainerColor
import map.getPrimaryContainerColor
import map.getTertiaryContainerColor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

lateinit var prefs: SharedPreferences

fun init(app: App) {
    prefs = app.getSharedPreferences("settings", MODE_PRIVATE)
}

var SharedPreferences.mapViewport: LatLngBounds
    get() {
        return LatLngBounds.from(
            latNorth = getFloat("latNorth", 12.116667f + 0.04f).toDouble(),
            lonEast = getFloat("lonEast", -68.93333f + 0.04f + 0.03f).toDouble(),
            latSouth = getFloat("latSouth", 12.116667f - 0.04f).toDouble(),
            lonWest = getFloat("lonWest", -68.93333f - 0.04f + 0.03f).toDouble(),
        )
    }
    set(value) {
        edit {
            putFloat("latNorth", value.latitudeNorth.toFloat())
            putFloat("lonEast", value.longitudeEast.toFloat())
            putFloat("latSouth", value.latitudeSouth.toFloat())
            putFloat("lonWest", value.longitudeWest.toFloat())
        }
    }

var SharedPreferences.mapStyle: MapStyle
    get() {
        return mapStyleFromPrefValue(getString("mapStyle", "auto") ?: "auto")
    }
    set(value) {
        edit {
            putString("mapStyle", value.toPrefValue())
        }
    }

enum class MapStyle {
    Auto,
    Liberty,
    Positron,
    Bright,
    Dark,
    CartoDarkMatter,
}

private fun mapStyleFromPrefValue(pref: String): MapStyle {
    return when (pref) {
        "auto" -> MapStyle.Auto
        "liberty" -> MapStyle.Liberty
        "positron" -> MapStyle.Positron
        "bright" -> MapStyle.Bright
        "dark" -> MapStyle.Dark
        "carto_dark_matter" -> MapStyle.CartoDarkMatter
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
        MapStyle.CartoDarkMatter -> "carto_dark_matter"
    }
}

fun MapStyle.name(context: Context): String {
    return when (this) {
        MapStyle.Auto -> context.getString(R.string.style_auto)
        MapStyle.Liberty -> context.getString(R.string.style_liberty)
        MapStyle.Positron -> context.getString(R.string.style_positron)
        MapStyle.Bright -> context.getString(R.string.style_bright)
        MapStyle.Dark -> context.getString(R.string.style_dark)
        MapStyle.CartoDarkMatter -> context.getString(R.string.style_carto_dark_matter)
    }
}

fun MapStyle.uri(context: Context): String {
    return when (this) {
        MapStyle.Auto -> {
            val nightMode =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            if (nightMode) {
                "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
            } else {
                "https://static.btcmap.org/map-styles/light.json"
            }
        }

        MapStyle.Liberty -> "https://tiles.openfreemap.org/styles/liberty"
        MapStyle.Positron -> "https://tiles.openfreemap.org/styles/positron"
        MapStyle.Bright -> "https://tiles.openfreemap.org/styles/bright"
        MapStyle.Dark -> "https://static.btcmap.org/map-styles/dark.json"
        MapStyle.CartoDarkMatter -> "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
    }
}

fun SharedPreferences.mapStyleIsDark(): Boolean {
    return when (mapStyle) {
        MapStyle.Dark, MapStyle.CartoDarkMatter -> true
        MapStyle.Auto -> {
            false
        }
        else -> false
    }
}

fun SharedPreferences.markerBackgroundColor(context: Context): Int {
    val customColor = getInt("markerBackgroundColor", -1)
    if (customColor != -1) return customColor

    if (useAdaptiveColors) {
        return context.getPrimaryContainerColor()
    }

    val isDark = mapStyleIsDark() ||
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFF0e95af.toInt() else 0xFF0e95af.toInt()
}

fun SharedPreferences.setMarkerBackgroundColor(color: Int?) {
    edit {
        if (color == null) {
            remove("markerBackgroundColor")
        } else {
            putInt(
                "markerBackgroundColor",
                color,
            )
        }
    }
}

private const val KEY_BOOSTED_MARKER_BACKGROUND_COLOR = "boostedMarkerBackgroundColor"

fun SharedPreferences.boostedMarkerBackgroundColor(): Int {
    return getInt(KEY_BOOSTED_MARKER_BACKGROUND_COLOR, "#f7931a".toColorInt())
}

fun SharedPreferences.setBoostedMarkerBackgroundColor(color: Int?) {
    edit {
        if (color == null) {
            remove(KEY_BOOSTED_MARKER_BACKGROUND_COLOR)
        } else {
            putInt(
                KEY_BOOSTED_MARKER_BACKGROUND_COLOR,
                color,
            )
        }
    }
}

fun SharedPreferences.markerIconColor(context: Context): Int {
    val customColor = getInt("markerIconColor", -1)
    if (customColor != -1) return customColor

    if (useAdaptiveColors) {
        return context.getOnPrimaryContainerColor()
    }

    val isDark = mapStyleIsDark() ||
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt() // White for both
}

fun SharedPreferences.setMarkerIconColor(color: Int?) {
    edit {
        if (color == null) {
            remove("markerIconColor")
        } else {
            putInt(
                "markerIconColor",
                color,
            )
        }
    }
}

fun SharedPreferences.badgeBackgroundColor(context: Context): Int {
    val customColor = getInt("badgeBackgroundColor", -1)
    if (customColor != -1) return customColor

    if (useAdaptiveColors) {
        return context.getOnPrimaryContainerColor()
    }

    val isDark = mapStyleIsDark() ||
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFF00a63e.toInt() else 0xFF00a63e.toInt()
}

fun SharedPreferences.setBadgeBackgroundColor(color: Int?) {
    edit {
        if (color == null) {
            remove("badgeBackgroundColor")
        } else {
            putInt(
                "badgeBackgroundColor",
                color,
            )
        }
    }
}

fun SharedPreferences.badgeTextColor(context: Context): Int {
    val customColor = getInt("badgeTextColor", -1)
    if (customColor != -1) return customColor

    if (useAdaptiveColors) {
        return context.getPrimaryContainerColor()
    }

    val isDark = mapStyleIsDark() ||
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt()
}

fun SharedPreferences.setBadgeTextColor(color: Int?) {
    edit {
        if (color == null) {
            remove("badgeTextColor")
        } else {
            putInt(
                "badgeTextColor",
                color,
            )
        }
    }
}

private const val KEY_API_URL = "apiUrl"

var SharedPreferences.apiUrl: HttpUrl
    get() {
        return getString(KEY_API_URL, "https://api.btcmap.org")!!.toHttpUrl()
    }
    set(value) {
        edit {
            putString(KEY_API_URL, value.toString())
        }
    }

fun SharedPreferences.apiUrlV4(vararg pathSegments: String): HttpUrl {
    val builder = apiUrl.newBuilder().addPathSegment("v4")
    pathSegments.forEach {
        builder.addPathSegment(it)
    }
    return builder.build()
}

private const val KEY_BUTTON_BACKGROUND_COLOR = "buttonBackgroundColor"

fun SharedPreferences.buttonBackgroundColor(context: Context): Int {
    val customColor = getInt(KEY_BUTTON_BACKGROUND_COLOR, -1)
    if (customColor != -1) return customColor

    if (useAdaptiveColors) {
        return context.getTertiaryContainerColor()
    }

    val isDark = mapStyleIsDark() ||
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFF1f2937.toInt() else 0xFF1f2937.toInt()
}

fun SharedPreferences.setButtonBackgroundColor(color: Int?) {
    edit {
        if (color == null) {
            remove(KEY_BUTTON_BACKGROUND_COLOR)
        } else {
            putInt(
                KEY_BUTTON_BACKGROUND_COLOR,
                color,
            )
        }
    }
}

private const val KEY_BUTTON_ICON_COLOR = "buttonIconColor"

fun SharedPreferences.buttonIconColor(context: Context): Int {
    val customColor = getInt(KEY_BUTTON_ICON_COLOR, -1)
    if (customColor != -1) return customColor

    if (useAdaptiveColors) {
        return context.getOnTertiaryContainerColor()
    }

    val isDark = mapStyleIsDark() ||
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt()
}

fun SharedPreferences.setButtonIconColor(color: Int?) {
    edit {
        if (color == null) {
            remove(KEY_BUTTON_ICON_COLOR)
        } else {
            putInt(
                KEY_BUTTON_ICON_COLOR,
                color,
            )
        }
    }
}

private const val KEY_BUTTON_BORDER_COLOR = "buttonBorderColor"

var SharedPreferences.useAdaptiveColors: Boolean
    get() = getBoolean("useAdaptiveColors", false)
    set(value) {
        edit { putBoolean("useAdaptiveColors", value) }
    }

fun SharedPreferences.buttonBorderColor(context: Context): Int {
    val customColor = getInt(KEY_BUTTON_BORDER_COLOR, -1)
    if (customColor != -1) return customColor

    if (useAdaptiveColors) {
        return context.getOnTertiaryContainerColor()
    }

    val isDark = mapStyleIsDark() ||
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    return if (isDark) 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt()
}

fun SharedPreferences.setButtonBorderColor(color: Int?) {
    edit {
        if (color == null) {
            remove(KEY_BUTTON_BORDER_COLOR)
        } else {
            putInt(
                KEY_BUTTON_BORDER_COLOR,
                color,
            )
        }
    }
}
