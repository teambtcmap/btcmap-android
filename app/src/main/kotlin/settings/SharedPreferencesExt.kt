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
}

private fun mapStyleFromPrefValue(pref: String): MapStyle {
    return when (pref) {
        "auto" -> MapStyle.Auto
        "liberty" -> MapStyle.Liberty
        "positron" -> MapStyle.Positron
        "bright" -> MapStyle.Bright
        "dark" -> MapStyle.Dark
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
    }
}

fun MapStyle.name(context: Context): String {
    return when (this) {
        MapStyle.Auto -> context.getString(R.string.style_auto)
        MapStyle.Liberty -> context.getString(R.string.style_liberty)
        MapStyle.Positron -> context.getString(R.string.style_positron)
        MapStyle.Bright -> context.getString(R.string.style_bright)
        MapStyle.Dark -> context.getString(R.string.style_dark)
    }
}

fun MapStyle.uri(context: Context): String {
    return when (this) {
        MapStyle.Auto -> {
            val nightMode =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            if (nightMode) {
                "https://static.btcmap.org/map-styles/dark.json"
            } else {
                "https://static.btcmap.org/map-styles/light.json"
            }
        }

        MapStyle.Liberty -> "https://tiles.openfreemap.org/styles/liberty"
        MapStyle.Positron -> "https://tiles.openfreemap.org/styles/positron"
        MapStyle.Bright -> "https://tiles.openfreemap.org/styles/bright"
        MapStyle.Dark -> "https://static.btcmap.org/map-styles/dark.json"
    }
}

fun SharedPreferences.markerBackgroundColor(): Int {
    return getInt("markerBackgroundColor", "#0e95af".toColorInt())
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

private val DEFAULT_MARKER_BACKGROUND_COLOR = "#f7931a".toColorInt()

fun SharedPreferences.boostedMarkerBackgroundColor(): Int {
    return getInt(KEY_BOOSTED_MARKER_BACKGROUND_COLOR, DEFAULT_MARKER_BACKGROUND_COLOR)
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

fun SharedPreferences.markerIconColor(): Int {
    return getInt("markerIconColor", Color.WHITE)
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

fun SharedPreferences.badgeBackgroundColor(): Int {
    return getInt("badgeBackgroundColor", "#00a63e".toColorInt())
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

fun SharedPreferences.badgeTextColor(): Int {
    return getInt("badgeTextColor", Color.WHITE)
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