package org.btcmap.settings

import org.junit.Assert
import org.junit.Test

/**
 * The offline download style mapping and its family grouping: the URL a pack is
 * fetched from must match the style the map is drawing, and two URLs that draw
 * the same map (light/dark, DarkMatter/Liberty) must read as one family.
 */
class OfflineStyleUrlTest {

    @Test
    fun auto_followsTheSystemTheme() {
        val light = MapStyle.Auto.offlineDownloadStyleUrl(darkSystemTheme = false)
        val dark = MapStyle.Auto.offlineDownloadStyleUrl(darkSystemTheme = true)
        Assert.assertNotEquals(light, dark)
        // Both draw the same sources, so a pack must not read as a different style.
        Assert.assertEquals(offlineStyleFamily(light), offlineStyleFamily(dark))
    }

    @Test
    fun darkMatter_downloadsThroughLiberty() {
        Assert.assertEquals(
            MapStyle.Liberty.offlineDownloadStyleUrl(darkSystemTheme = false),
            MapStyle.DarkMatter.offlineDownloadStyleUrl(darkSystemTheme = false),
        )
    }

    @Test
    fun differentStyles_keepDistinctFamilies() {
        val liberty = MapStyle.Liberty.offlineDownloadStyleUrl(darkSystemTheme = false)
        val positron = MapStyle.Positron.offlineDownloadStyleUrl(darkSystemTheme = false)
        Assert.assertNotEquals(offlineStyleFamily(liberty), offlineStyleFamily(positron))
    }
}
