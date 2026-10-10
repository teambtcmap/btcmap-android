package org.btcmap.ui.map

import android.content.Context
import android.content.res.AssetManager
import java.io.FileNotFoundException
import org.maplibre.compose.map.DefaultMapRuntime

/**
 * Serves `app://map-styles/...` (sprite sheets and glyph ranges) from the app's
 * assets. Call once, before the first map is created.
 *
 * The provider itself is shared with the desktop host
 * ([bundledMapResourceProvider]); only the asset reader is Android-specific.
 */
fun configureBundledMapResources(context: Context) {
    val assets = context.applicationContext.assets
    DefaultMapRuntime.configure {
        resourceProvider = bundledMapResourceProvider(assets::readBytesOrNull)
    }
}

/**
 * The bundled style at [assetPath] with its `asset://` sprite and glyph URLs
 * rewritten to [BUNDLED_MAP_SCHEME].
 */
fun bundledStyleJson(context: Context, assetPath: String): String =
    bundledStyleJson(context.assets::readBytesOrNull, assetPath)

/**
 * [bundledStyleJson] for a style [styleUrl], or null when the style is not a
 * bundled asset one (so the caller can pass the URL through instead).
 */
fun bundledStyleJsonFor(context: Context, styleUrl: String): String? =
    if (styleUrl.startsWith("asset://")) {
        bundledStyleJson(context, styleUrl.removePrefix("asset://"))
    } else {
        null
    }

private fun AssetManager.readBytesOrNull(path: String): ByteArray? =
    try {
        open(path).readBytes()
    } catch (_: FileNotFoundException) {
        null
    }
