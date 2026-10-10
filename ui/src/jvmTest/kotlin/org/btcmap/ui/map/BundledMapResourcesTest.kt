package org.btcmap.ui.map

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * The bundled style URL rewrite is shared by the Android and desktop hosts now,
 * so the rewrite both rely on is pinned here.
 */
class BundledMapResourcesTest {

    @Test
    fun rewritesAssetStyleUrlsToBundledScheme() {
        val source =
            """{"sprite":"asset://map-styles/light/sprite",""" +
                """"glyphs":"asset://map-styles/glyphs/{fontstack}/{range}.pbf"}"""

        val json = bundledStyleJson({ source.encodeToByteArray() }, "map-styles/light/style.json")

        assertContains(json, "app://map-styles/")
        assertTrue("asset://map-styles/" !in json)
    }
}
