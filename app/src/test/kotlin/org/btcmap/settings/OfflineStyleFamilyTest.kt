package org.btcmap.settings

import org.junit.Assert
import org.junit.Test

class OfflineStyleFamilyTest {

    @Test
    fun theAutoLightAndDarkStylesShareOneFamily() {
        val light = offlineStyleFamily("https://static.btcmap.org/map-styles/light.json")
        val dark = offlineStyleFamily("https://static.btcmap.org/map-styles/dark.json")

        // Auto resolves to one or the other with the system theme; a pack must
        // not read as a different style just because dark mode turned on.
        Assert.assertEquals(light, dark)
    }

    @Test
    fun differentNamedStylesKeepDistinctFamilies() {
        val liberty = offlineStyleFamily("https://tiles.openfreemap.org/styles/liberty")
        val positron = offlineStyleFamily("https://tiles.openfreemap.org/styles/positron")

        Assert.assertNotEquals(liberty, positron)
    }

    @Test
    fun unknownUrlsKeepTheirOwnIdentity() {
        val url = "https://example.com/custom/style.json"

        Assert.assertEquals(url, offlineStyleFamily(url))
    }
}
