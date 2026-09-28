package org.btcmap.map

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.btcmap.bundle.BundledBasemap
import java.io.IOException

/**
 * A style rewritten to use the bundled basemap as an offline fallback.
 */
internal data class BundledBasemapStyle(
    val json: String,
    /**
     * The bundled fallback layers. Hidden while online and shown offline,
     * underneath the hosted layers, so a tile MapLibre already cached is drawn
     * in full detail and only what is not cached falls back to the overzoomed
     * archive.
     */
    val fallbackLayers: List<String>,
)

/**
 * Rewrites an OpenFreeMap style so the bundled low-zoom archive can stand in
 * for the hosted tiles while offline.
 *
 * The style's `openmaptiles` source is renamed to `openmaptiles-remote` and
 * left on the network at every zoom, so online, and offline for anything
 * MapLibre has cached, the map renders exactly as before. A second
 * `openmaptiles-bundled` source reads the archive, and the geometry layers get
 * `-bundled` copies in a block below the hosted ones, hidden until the network
 * is gone. Those copies are uncapped, so the archive's z[MAX_ZOOM] tiles
 * overzoom to fill whatever the cache is missing.
 *
 * Symbol (label) layers get no fallback copy: a cached hosted tile already
 * draws its labels, and a second overzoomed set underneath would double them.
 * Labels therefore come from the archive only at the low zooms the archive
 * covers natively, and from the cache at higher ones.
 *
 * Returns null when the style has no `openmaptiles` source or cannot be parsed,
 * in which case the caller keeps the unmodified style URI.
 */
internal fun rewriteForBundledBasemap(styleJson: String, pmtilesUrl: String): BundledBasemapStyle? {
    val style = try {
        JsonParser.parseString(styleJson).asJsonObject
    } catch (_: JsonSyntaxException) {
        return null
    } catch (_: IllegalStateException) {
        // parseString returns a JsonElement; a non-object style fails here.
        return null
    }

    val sources = style.getAsJsonObject("sources") ?: return null
    val vector = sources.getAsJsonObject(OPENMAPTILES) ?: return null

    sources.remove(OPENMAPTILES)
    sources.add(OPENMAPTILES_REMOTE, vector.deepCopy())

    val bundled = JsonObject().apply {
        addProperty("type", "vector")
        // A `tiles` entry rather than a `url`: the archive answers tile requests
        // itself, and the template carries no placeholders.
        add("tiles", JsonArray().apply { add(pmtilesUrl) })
        addProperty("minzoom", 0)
        addProperty("maxzoom", BundledBasemap.MAX_ZOOM)
    }
    sources.add(OPENMAPTILES_BUNDLED, bundled)

    val layers = style.getAsJsonArray("layers") ?: return null

    // Geometry-only fallback copies, built first so they can be placed as one
    // block below the hosted layers.
    val fallback = JsonArray()
    val fallbackLayers = mutableListOf<String>()
    for (element in layers) {
        val layer = element.asJsonObject
        if (!layer.drawsFromOpenMapTiles() || layer.isSymbol()) continue

        val clone = layer.deepCopy()
        clone.addProperty("id", "${layer.get("id").asString}-bundled")
        clone.addProperty("source", OPENMAPTILES_BUNDLED)
        clone.addProperty("visibility", VISIBILITY_NONE)
        fallback.add(clone)
        fallbackLayers.add(clone.get("id").asString)
    }

    // The hosted layers keep their original order; the fallback block goes in
    // just before the first of them, so it sits under the whole hosted stack.
    val rewritten = JsonArray()
    var fallbackInserted = false
    for (element in layers) {
        val layer = element.asJsonObject
        if (layer.drawsFromOpenMapTiles()) {
            if (!fallbackInserted) {
                fallback.forEach { rewritten.add(it) }
                fallbackInserted = true
            }
            layer.addProperty("source", OPENMAPTILES_REMOTE)
        }
        rewritten.add(layer)
    }

    style.add("layers", rewritten)
    return BundledBasemapStyle(
        json = Gson().toJson(style),
        fallbackLayers = fallbackLayers,
    )
}

/**
 * Reads the style from `assets` and rewrites it, or returns null when [styleUri]
 * is not an asset URI or the rewrite does not apply.
 */
internal fun bundledBasemapStyleJson(
    context: Context,
    styleUri: String,
    pmtilesUrl: String,
): BundledBasemapStyle? {
    val assetPath = styleUri.removePrefix(ASSET_SCHEME)
    if (assetPath == styleUri) return null

    val json = try {
        context.assets.open(assetPath).bufferedReader().use { it.readText() }
    } catch (_: IOException) {
        return null
    }

    return rewriteForBundledBasemap(json, pmtilesUrl)
}

private fun JsonObject.drawsFromOpenMapTiles(): Boolean {
    val source = get("source") ?: return false
    return source.isJsonPrimitive && source.asString == OPENMAPTILES
}

private fun JsonObject.isSymbol(): Boolean =
    get("type")?.takeIf { it.isJsonPrimitive }?.asString == "symbol"

private const val ASSET_SCHEME = "asset://"
private const val OPENMAPTILES = "openmaptiles"
private const val OPENMAPTILES_BUNDLED = "openmaptiles-bundled"
private const val OPENMAPTILES_REMOTE = "openmaptiles-remote"
private const val VISIBILITY_NONE = "none"
