package org.btcmap.ui.map

import android.content.Context
import android.content.res.AssetManager
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.resource.MapResourceProvider

/**
 * The URL scheme the bundled map resources are served under. The app's styles
 * use MapLibre's `asset://` scheme, which the Compose map cannot read, so they
 * are rewritten to this scheme and served from the assets below.
 */
const val BUNDLED_MAP_SCHEME = "app"

/** Where [bundledStyleJson]'s `asset://map-styles/` URLs live in the assets. */
private const val MAP_STYLES_PREFIX = "map-styles/"

private const val GLYPH_PATH_PREFIX = "${MAP_STYLES_PREFIX}glyphs/"

/**
 * The manifest the glyph bundler writes next to the ranges: the host the ranges
 * were downloaded from, so the ones the bundle deliberately omits can still be
 * fetched. See `bundle_map_styles.py`.
 */
private const val GLYPH_SOURCE_ASSET = "${GLYPH_PATH_PREFIX}source.json"

/** How long a single online glyph range fetch may take before it gives up. */
private const val GLYPH_FETCH_TIMEOUT_MS = 10_000

/**
 * Serves `app://map-styles/...` (sprite sheets and glyph ranges) from the app's
 * assets. Call once, before the first map is created.
 *
 * The glyph bundle omits the CJK ideograph and Hangul blocks (they are ~90 MB on
 * their own), so a range the styles need there is fetched from the glyph host
 * recorded in [GLYPH_SOURCE_ASSET] instead of failing. When that fetch fails
 * (offline), an empty glyph range is served so the vector tile still renders:
 * MapLibre Native drops a whole tile whose glyph request errors (see
 * maplibre-native#4430), which would otherwise leave a blank basemap over those
 * regions even though the tile's roads and polygons loaded fine.
 */
fun configureBundledMapResources(context: Context) {
    val assets = context.applicationContext.assets
    val glyphBase = glyphSourceBase(assets)
    val glyphCache = ConcurrentHashMap<String, ByteArray>()

    val provider = MapResourceProvider(scheme = BUNDLED_MAP_SCHEME) { request ->
        // Glyph paths arrive percent-encoded ("Noto%20Sans%20Italic"), which is
        // not how the asset is named.
        val encodedPath = request.url.removePrefix("$BUNDLED_MAP_SCHEME://")
        val path = URLDecoder.decode(encodedPath, Charsets.UTF_8.name())
        try {
            assets.open(path).readBytes()
        } catch (e: FileNotFoundException) {
            if (!path.startsWith(GLYPH_PATH_PREFIX)) throw e

            glyphCache.getOrPut(encodedPath) {
                fetchGlyphRange(glyphBase, encodedPath) ?: emptyGlyphRange(path = path)
            }
        }
    }
    DefaultMapRuntime.configure { resourceProvider = provider }
}

/**
 * The base URL the bundled glyph ranges were downloaded from, or null when the
 * manifest is missing or malformed, in which case only the bundled ranges can be
 * served.
 */
private fun glyphSourceBase(assets: AssetManager): String? = try {
    val json = assets.open(GLYPH_SOURCE_ASSET).readBytes().decodeToString()
    JSONObject(json).optString("base").takeIf { it.isNotBlank() }
} catch (_: Exception) {
    null
}

/**
 * The glyph range at [encodedPath] (an encoded `map-styles/glyphs/<fontstack>/
 * <range>.pbf`), fetched from [base], or null when it could not be fetched.
 */
private fun fetchGlyphRange(base: String?, encodedPath: String): ByteArray? {
    if (base == null) return null

    val relative = encodedPath.removePrefix(GLYPH_PATH_PREFIX)
    return try {
        val url = URI(base).resolve(relative).toURL()
        (url.openConnection() as HttpURLConnection).run {
            connectTimeout = GLYPH_FETCH_TIMEOUT_MS
            readTimeout = GLYPH_FETCH_TIMEOUT_MS
            try {
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream.use { it.readBytes() }
                } else {
                    null
                }
            } finally {
                disconnect()
            }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * [emptyGlyphRange] for the range in [path] (a decoded
 * `map-styles/glyphs/<fontstack>/<range>.pbf`), so a tile that needs glyphs the
 * app could not load still renders its labels-free geometry.
 */
private fun emptyGlyphRange(path: String): ByteArray {
    val file = path.removePrefix(GLYPH_PATH_PREFIX)
    return emptyGlyphRange(
        fontstack = file.substringBeforeLast('/'),
        range = file.substringAfterLast('/').removeSuffix(".pbf"),
    )
}

/**
 * The bundled style at [assetPath] with its `asset://` sprite and glyph URLs
 * rewritten to [BUNDLED_MAP_SCHEME] so [configureBundledMapResources] can serve
 * them.
 */
fun bundledStyleJson(context: Context, assetPath: String): String =
    context.assets.open(assetPath).readBytes().decodeToString()
        .replace("asset://$MAP_STYLES_PREFIX", "$BUNDLED_MAP_SCHEME://$MAP_STYLES_PREFIX")

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
