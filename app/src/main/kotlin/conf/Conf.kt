package conf

import android.content.Context
import android.content.res.Configuration
import org.btcmap.R

data class Conf(
    val showAtms: Boolean,
    val mapStyle: MapStyle,
)

enum class MapStyle {
    Auto,
    Liberty,
    Positron,
    Bright,
    Dark,
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