package org.btcmap.ui.map

import android.content.Context
import java.net.URLDecoder
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MapRuntimeOptions
import org.maplibre.compose.resource.MapResourceProvider

/**
 * The URL scheme the bundled map resources are served under. The app's styles
 * use MapLibre's `asset://` scheme, which the Compose map cannot read, so they
 * are rewritten to this scheme and served from the assets below.
 */
const val BUNDLED_MAP_SCHEME = "app"

/**
 * Serves `app://map-styles/...` (sprite sheets and glyph ranges) from the app's
 * assets. Call once, before the first map is created.
 */
fun configureBundledMapResources(context: Context) {
    val assets = context.applicationContext.assets
    val provider = MapResourceProvider(scheme = BUNDLED_MAP_SCHEME) { request ->
        // Glyph paths arrive percent-encoded ("Noto%20Sans%20Italic"), which is
        // not how the asset is named.
        val path = URLDecoder.decode(
            request.url.removePrefix("$BUNDLED_MAP_SCHEME://"),
            Charsets.UTF_8.name(),
        )
        assets.open(path).readBytes()
    }
    DefaultMapRuntime.configure(MapRuntimeOptions(resourceProvider = provider))
}

/**
 * The bundled style at [assetPath] with its `asset://` sprite and glyph URLs
 * rewritten to [BUNDLED_MAP_SCHEME] so [configureBundledMapResources] can serve
 * them.
 */
fun bundledStyleJson(context: Context, assetPath: String): String =
    context.assets.open(assetPath).readBytes().decodeToString()
        .replace("asset://map-styles/", "$BUNDLED_MAP_SCHEME://map-styles/")
