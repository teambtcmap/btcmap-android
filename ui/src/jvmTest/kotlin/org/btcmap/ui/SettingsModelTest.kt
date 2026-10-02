package org.btcmap.ui

import androidx.compose.ui.graphics.Color
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.btcmap.settings.MapColor
import org.btcmap.settings.Settings
import org.btcmap.settings.setMapColor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The shared settings model: the row list both hosts render and the colour rows
 * built from the shared registry.
 */
class SettingsModelTest {

    private fun settings(): Settings {
        val db = Database(BundledSQLiteDriver(), ":memory:")
        return Settings(dbProvider = { db }, legacyValues = { emptyMap() })
    }

    @Test
    fun settingsItems_areTheAndroidRowsInOrder() {
        val items = settingsItems(strings(), showAttribution = true, mapRotationEnabled = false)

        assertEquals(
            listOf(
                "account",
                "mapStyle",
                "customizeColors",
                "verifiedFilter",
                "showAttribution",
                "mapRotation",
                "dbStats",
                "imageStats",
            ),
            items.map { it.key },
        )
    }

    @Test
    fun settingsItems_omitImageStatsForTheDesktop() {
        val items = settingsItems(
            strings(),
            showAttribution = true,
            mapRotationEnabled = false,
            includeImageStats = false,
        )

        assertEquals(
            listOf(
                "account",
                "mapStyle",
                "customizeColors",
                "verifiedFilter",
                "showAttribution",
                "mapRotation",
                "dbStats",
            ),
            items.map { it.key },
        )
    }

    @Test
    fun mapColorItems_comeFromTheSharedRegistry() {
        val settings = settings()
        settings.setMapColor(MapColor.MarkerBackground, 0xFF112233.toInt())

        val items = mapColorItems(settings) { it.key }

        assertEquals(MapColor.entries.map { it.key }, items.map { it.key })
        val marker = items.first { it.key == MapColor.MarkerBackground.key }
        assertEquals("#FF112233", marker.value)
        assertEquals(Color(0xFF112233.toInt()), marker.color)
    }

    private fun strings() = SettingsStrings(
        accountTitle = "account",
        accountSecondary = "account-secondary",
        mapStyle = "map-style",
        mapStyleValue = "map-style-value",
        customizeColors = "customize-colors",
        customizeColorsSecondary = "customize-colors-secondary",
        verifiedFilter = "verified-filter",
        verifiedFilterValue = "verified-filter-value",
        showAttribution = "show-attribution",
        showAttributionSecondary = "show-attribution-secondary",
        mapRotation = "map-rotation",
        mapRotationSecondary = "map-rotation-secondary",
        dbStats = "db-stats",
        dbStatsSecondary = "db-stats-secondary",
        imageStats = "image-stats",
        imageStatsSecondary = "image-stats-secondary",
    )
}
