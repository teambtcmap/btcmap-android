package org.btcmap.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.core.content.edit
import org.btcmap.BuildConfig
import org.btcmap.R
import org.maplibre.android.geometry.LatLngBounds
import androidx.core.graphics.toColorInt
import org.btcmap.App
import org.btcmap.auth.TokenCipher
import org.btcmap.map.getOnPrimaryContainerColor
import org.btcmap.map.getOnTertiaryContainerColor
import org.btcmap.map.getPrimaryContainerColor
import org.btcmap.map.getTertiaryContainerColor
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
                "asset://map-styles/carto-dark-matter/style.json"
            } else {
                "asset://map-styles/light/style.json"
            }
        }

        MapStyle.Liberty -> "asset://map-styles/liberty/style.json"
        MapStyle.Positron -> "asset://map-styles/positron/style.json"
        MapStyle.Bright -> "asset://map-styles/bright/style.json"
        MapStyle.Dark -> "asset://map-styles/dark/style.json"
        MapStyle.CartoDarkMatter -> "asset://map-styles/carto-dark-matter/style.json"
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

internal const val KEY_AUTH_TOKEN = "auth_token"

var SharedPreferences.authToken: String?
    get() {
        val stored = getString(KEY_AUTH_TOKEN, null) ?: return null

        if (!stored.startsWith(TokenCipher.ENCODED_PREFIX)) {
            // Upgrade a token stored in plaintext by an older app version. If the
            // keystore is unavailable the token is still returned unencrypted
            // rather than breaking the session.
            runCatching {
                val encoded = TokenCipher.encrypt(stored)
                edit { putString(KEY_AUTH_TOKEN, encoded) }
                TokenCipher.rememberDecrypted(encoded, stored)
            }
            return stored
        }

        return when (val result = TokenCipher.decrypt(stored)) {
            is TokenCipher.DecryptResult.Success -> result.plaintext

            TokenCipher.DecryptResult.Unrecoverable -> {
                // The keystore entry that encrypted the token is gone, so it can
                // never be recovered. Drop it and fall back to a signed-out state
                // instead of retrying the keystore on every request; a following
                // 401 also clears the cached user. Re-read first so a concurrent
                // sign-in is not lost.
                if (getString(KEY_AUTH_TOKEN, null) == stored) {
                    runCatching {
                        edit { remove(KEY_AUTH_TOKEN) }
                    }
                }
                null
            }

            // The keystore is temporarily unavailable: keep the ciphertext so a
            // later request can recover the session.
            TokenCipher.DecryptResult.Unavailable -> null
        }
    }
    set(value) {
        if (value == null) {
            // Do not keep the decrypted token in memory after signing out.
            TokenCipher.clearCache()
            edit { remove(KEY_AUTH_TOKEN) }
        } else {
            val encoded = TokenCipher.encrypt(value)
            edit { putString(KEY_AUTH_TOKEN, encoded) }
            // Prime the cache so the freshly signed-in token is not read back
            // from the keystore on the next access, including from the main thread.
            TokenCipher.rememberDecrypted(encoded, value)
        }
    }

/**
 * Raw stored token value. Used by session rollback, which must compare and
 * restore the exact stored bytes without depending on the keystore being
 * readable.
 */
internal fun SharedPreferences.getStoredAuthToken(): String? = getString(KEY_AUTH_TOKEN, null)

/**
 * Replaces the raw stored token and drops the decrypt cache, which still holds
 * the plaintext of the value that was just replaced.
 */
internal fun SharedPreferences.setStoredAuthToken(encoded: String?) {
    TokenCipher.clearCache()
    edit {
        if (encoded == null) remove(KEY_AUTH_TOKEN) else putString(KEY_AUTH_TOKEN, encoded)
    }
}

/**
 * Restores the raw stored token to [previous] only while it is still [current],
 * reporting whether it changed. A concurrent sign-in that stored a newer value
 * wins, and comparing raw bytes means a temporarily unreadable keystore cannot
 * cause a recoverable token to be discarded.
 */
internal fun SharedPreferences.restoreStoredAuthTokenIf(
    current: String?,
    previous: String?,
): Boolean {
    return runCatching {
        if (getStoredAuthToken() == current) {
            setStoredAuthToken(previous)
            true
        } else {
            // A concurrent sign-in stored a newer token; do not retain the plaintext
            // of the failed one in the cache.
            current?.let(TokenCipher::clearCacheIf)
            false
        }
    }.getOrDefault(false)
}

val SharedPreferences.authorized: Boolean
    get() {
        if (!authToken.isNullOrBlank()) return true

        // A null token does not necessarily mean "signed out": the keystore may be
        // temporarily unavailable while the encrypted token is still stored. Keep
        // reporting a session in that case so the UI does not force a re-sign-in
        // that would overwrite a token that can still recover.
        return getString(KEY_AUTH_TOKEN, null) != null
    }

private const val KEY_SHOW_DEBUG_INFO = "show_debug_info"

var SharedPreferences.showDebugInfo: Boolean
    get() = getBoolean(KEY_SHOW_DEBUG_INFO, BuildConfig.DEBUG)
    set(value) {
        edit { putBoolean(KEY_SHOW_DEBUG_INFO, value) }
    }

private const val KEY_SHOW_ATTRIBUTION = "show_attribution"

var SharedPreferences.showAttribution: Boolean
    get() = getBoolean(KEY_SHOW_ATTRIBUTION, true)
    set(value) {
        edit { putBoolean(KEY_SHOW_ATTRIBUTION, value) }
    }

private const val KEY_MAP_ROTATION_ENABLED = "map_rotation_enabled"

var SharedPreferences.mapRotationEnabled: Boolean
    get() = getBoolean(KEY_MAP_ROTATION_ENABLED, false)
    set(value) {
        edit { putBoolean(KEY_MAP_ROTATION_ENABLED, value) }
    }

private const val KEY_VERIFIED_FILTER_YEARS = "verified_filter_years"

var SharedPreferences.verifiedFilterYears: Int
    get() {
        return getInt(KEY_VERIFIED_FILTER_YEARS, 3)
    }
    set(value) {
        edit {
            putInt(KEY_VERIFIED_FILTER_YEARS, value)
        }
    }

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

var SharedPreferences.activityIntervalDays: Int
    get() = getInt(KEY_ACTIVITY_INTERVAL_DAYS, ActivityInterval.HalfYear.days)
    set(value) {
        edit { putInt(KEY_ACTIVITY_INTERVAL_DAYS, value) }
    }