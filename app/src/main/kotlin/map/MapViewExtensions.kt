package map

import android.content.Context
import android.content.res.Configuration
import org.maplibre.android.maps.Style

fun styleBuilder(context: Context): Style.Builder {
    val nightMode =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    val uri = if (nightMode) {
        "asset://dark.json"
    } else {
        "asset://light.json"
    }

    return Style.Builder().fromUri(uri)
}