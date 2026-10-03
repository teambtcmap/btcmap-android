package org.btcmap.settings

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test

/**
 * The shared colour registry: one definition of the stored keys and defaults
 * that both hosts read, so Android and the desktop can no longer drift.
 */
class MapColorsTest {

    private fun settings(): Settings {
        val db = org.btcmap.db.testDatabase()
        return Settings(dbProvider = { db }, legacyValues = { emptyMap() })
    }

    @Test
    fun defaultsComeFromTheRegistry() {
        val settings = settings()

        MapColor.entries.forEach { color ->
            Assert.assertEquals(color.defaultArgb, settings.mapColor(color))
        }
    }

    @Test
    fun overrideRoundTripsAndResetClearsIt() {
        val settings = settings()

        settings.setMapColor(MapColor.MarkerBackground, 0xFF112233.toInt())
        Assert.assertEquals(0xFF112233.toInt(), settings.mapColor(MapColor.MarkerBackground))

        settings.setMapColor(MapColor.MarkerBackground, null)
        Assert.assertEquals(MapColor.MarkerBackground.defaultArgb, settings.mapColor(MapColor.MarkerBackground))
    }

    @Test
    fun fromKey_findsAColorAndRejectsTheRest() {
        Assert.assertEquals(MapColor.BadgeText, MapColor.fromKey("badgeTextColor"))
        Assert.assertNull(MapColor.fromKey("notAColor"))
    }
}
