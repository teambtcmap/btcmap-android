package map

import android.content.Context
import android.content.res.Configuration
import conf.Conf
import org.maplibre.android.maps.Style

fun styleBuilder(context: Context, conf: Conf): Style.Builder {
    if (conf.mapStyleUrl == null) {
        val nightMode =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        val uri = if (nightMode) {
            "asset://dark.json"
        } else {
            "asset://light.json"
        }

        return Style.Builder().fromUri(uri)
    } else {
        return Style.Builder().fromUri(conf.mapStyleUrl.toString())
    }
}