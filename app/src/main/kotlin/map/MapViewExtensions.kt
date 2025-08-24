package map

import android.content.Context
import conf.Conf
import conf.uri
import org.maplibre.android.maps.Style

fun styleBuilder(context: Context, conf: Conf): Style.Builder {
    return Style.Builder().fromUri(conf.mapStyle.uri(context))
}