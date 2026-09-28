package org.btcmap.map

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert
import org.junit.Test

class BundledBasemapStyleTest {

    private val pmtilesUrl = "pmtiles://file:///data/user/0/org.btcmap/files/basemap.pmtiles"

    private val style = """
        {
          "version": 8,
          "sources": {
            "ne2_shaded": {
              "type": "raster",
              "tiles": ["https://tiles.openfreemap.org/natural_earth/ne2sr/{z}/{x}/{y}.png"]
            },
            "openmaptiles": {
              "type": "vector",
              "url": "https://tiles.openfreemap.org/planet"
            }
          },
          "layers": [
            {"id": "background", "type": "background"},
            {"id": "water", "type": "fill", "source": "openmaptiles", "source-layer": "water"},
            {"id": "road", "type": "line", "source": "openmaptiles", "minzoom": 5, "maxzoom": 12},
            {"id": "country_label", "type": "symbol", "source": "openmaptiles", "maxzoom": 4},
            {"id": "place_label", "type": "symbol", "source": "openmaptiles", "source-layer": "place"},
            {"id": "relief", "type": "raster", "source": "ne2_shaded"}
          ]
        }
    """.trimIndent()

    private fun rewrite(): BundledBasemapStyle {
        val rewritten = rewriteForBundledBasemap(style, pmtilesUrl)
        Assert.assertNotNull(rewritten)
        return rewritten!!
    }

    private fun BundledBasemapStyle.json(): JsonObject =
        JsonParser.parseString(json).asJsonObject

    private fun JsonObject.layers(): List<JsonObject> =
        getAsJsonArray("layers").map { it.asJsonObject }

    private fun JsonObject.layer(id: String): JsonObject? =
        layers().firstOrNull { it.get("id").asString == id }

    private fun JsonObject.indexOf(id: String): Int =
        layers().indexOfFirst { it.get("id").asString == id }

    @Test
    fun theHostedSourceIsKeptAndTheArchiveAdded() {
        val sources = rewrite().json().getAsJsonObject("sources")

        Assert.assertFalse(sources.has("openmaptiles"))

        val hosted = sources.getAsJsonObject("openmaptiles-remote")
        Assert.assertEquals("https://tiles.openfreemap.org/planet", hosted.get("url").asString)

        val bundled = sources.getAsJsonObject("openmaptiles-bundled")
        Assert.assertEquals("vector", bundled.get("type").asString)
        Assert.assertEquals(pmtilesUrl, bundled.getAsJsonArray("tiles")[0].asString)
        Assert.assertEquals(0, bundled.get("minzoom").asInt)
        Assert.assertEquals(4, bundled.get("maxzoom").asInt)
    }

    @Test
    fun everyLayerKeepsItsHostedSourceUnchanged() {
        val style = rewrite().json()

        // No zoom splitting: the hosted layers render exactly as before so
        // cached tiles look identical online and offline.
        val water = style.layer("water")!!
        Assert.assertEquals("openmaptiles-remote", water.get("source").asString)
        Assert.assertFalse(water.has("maxzoom"))

        val road = style.layer("road")!!
        Assert.assertEquals("openmaptiles-remote", road.get("source").asString)
        Assert.assertEquals(5, road.get("minzoom").asInt)
        Assert.assertEquals(12, road.get("maxzoom").asInt)
    }

    @Test
    fun geometryLayersGetAHiddenBundledFallback() {
        val style = rewrite().json()

        val water = style.layer("water-bundled")!!
        Assert.assertEquals("openmaptiles-bundled", water.get("source").asString)
        Assert.assertEquals("none", water.get("visibility").asString)

        val road = style.layer("road-bundled")!!
        Assert.assertEquals("openmaptiles-bundled", road.get("source").asString)
        Assert.assertEquals("none", road.get("visibility").asString)
    }

    @Test
    fun symbolLayersGetNoFallbackSoLabelsAreNotDoubled() {
        val style = rewrite().json()

        Assert.assertEquals("openmaptiles-remote", style.layer("country_label")!!.get("source").asString)
        Assert.assertEquals("openmaptiles-remote", style.layer("place_label")!!.get("source").asString)
        Assert.assertNull(style.layer("country_label-bundled"))
        Assert.assertNull(style.layer("place_label-bundled"))
    }

    @Test
    fun theFallbackBlockSitsBelowEveryHostedLayer() {
        val style = rewrite().json()

        // Both fallbacks precede the first hosted layer, so the hosted stack
        // (and any cached tile in it) always draws on top.
        Assert.assertTrue(style.indexOf("water-bundled") < style.indexOf("water"))
        Assert.assertTrue(style.indexOf("road-bundled") < style.indexOf("road"))
        Assert.assertTrue(style.indexOf("road-bundled") < style.indexOf("country_label"))
    }

    @Test
    fun theFallbackLayerIdsAreReported() {
        Assert.assertEquals(
            listOf("water-bundled", "road-bundled"),
            rewrite().fallbackLayers,
        )
    }

    @Test
    fun layersFromOtherSourcesAreUntouched() {
        val style = rewrite().json()

        Assert.assertEquals("ne2_shaded", style.layer("relief")!!.get("source").asString)
        Assert.assertNull(style.layer("background")!!.get("source"))
    }

    @Test
    fun aStyleWithoutOpenFreeMapIsLeftAlone() {
        val other = """{"version":8,"sources":{"vector":{"type":"vector","url":"https://x"}},"layers":[]}"""

        Assert.assertNull(rewriteForBundledBasemap(other, pmtilesUrl))
    }

    @Test
    fun malformedInputReturnsNull() {
        Assert.assertNull(rewriteForBundledBasemap("not json", pmtilesUrl))
        Assert.assertNull(rewriteForBundledBasemap("[]", pmtilesUrl))
    }
}
