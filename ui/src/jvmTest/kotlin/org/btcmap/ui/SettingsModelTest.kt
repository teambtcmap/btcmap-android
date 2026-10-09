package org.btcmap.ui

import androidx.compose.ui.graphics.Color
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.btcmap.settings.MapColor
import org.btcmap.settings.Settings
import org.btcmap.settings.setMapColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun settingsItems_areTheAndroidEntriesInOrder() {
        val items = settingsItems(strings(), showAttribution = true, mapRotationEnabled = false, mapTiltEnabled = false)

        assertEquals(
            listOf(
                "account",
                "headerGeneral",
                "language",
                "headerMap",
                "mapStyle",
                "customizeColors",
                "verifiedFilter",
                "showAttribution",
                "mapRotation",
                "mapTilt",
                "headerData",
                "dbStats",
                "imageStats",
            ),
            items.map { it.key },
        )
    }

    @Test
    fun settingsItems_includeManageAreasOnlyForAreaAdmins() {
        val items = settingsItems(
            strings(),
            showAttribution = true,
            mapRotationEnabled = false,
            mapTiltEnabled = false,
            includeManageAreas = true,
        )

        assertEquals("manageAreas", items.last().key)
        assertEquals(
            "manage-areas",
            (items.last() as SettingsItem.Action).title,
        )

        assertTrue(
            settingsItems(strings(), showAttribution = true, mapRotationEnabled = false, mapTiltEnabled = false)
                .none { it.key == "manageAreas" },
        )
    }

    @Test
    fun settingsItems_includeManagePlaceImagesOnlyWhenAsked() {
        val items = settingsItems(
            strings(),
            showAttribution = true,
            mapRotationEnabled = false,
            mapTiltEnabled = false,
            includeManagePlaceImages = true,
        )

        assertEquals("managePlaceImages", items.last().key)
        assertEquals(
            "manage-place-images",
            (items.last() as SettingsItem.Action).title,
        )
        // The shared Admin header appears with the row.
        assertTrue(items.any { it.key == "headerAdmin" })

        assertTrue(
            settingsItems(strings(), showAttribution = true, mapRotationEnabled = false, mapTiltEnabled = false)
                .none { it.key == "managePlaceImages" },
        )
    }

    @Test
    fun settingsItems_addTheAdminHeaderOnceForBothAdminRows() {
        val items = settingsItems(
            strings(),
            showAttribution = true,
            mapRotationEnabled = false,
            mapTiltEnabled = false,
            includeManageAreas = true,
            includeManagePlaceImages = true,
        )

        assertEquals(1, items.count { it.key == "headerAdmin" })
        assertEquals(
            listOf("manageAreas", "managePlaceImages"),
            items.takeLast(2).map { it.key },
        )
    }

    @Test
    fun settingsItems_omitImageStatsForTheDesktop() {
        val items = settingsItems(
            strings(),
            showAttribution = true,
            mapRotationEnabled = false,
            mapTiltEnabled = false,
            includeImageStats = false,
        )

        assertEquals(
            listOf(
                "account",
                "headerGeneral",
                "language",
                "headerMap",
                "mapStyle",
                "customizeColors",
                "verifiedFilter",
                "showAttribution",
                "mapRotation",
                "mapTilt",
                "headerData",
                "dbStats",
            ),
            items.map { it.key },
        )
    }

    @Test
    fun settingsItems_giveEveryRowAnIcon() {
        val items = settingsItems(
            strings(),
            showAttribution = true,
            mapRotationEnabled = false,
            mapTiltEnabled = false,
            includeManageAreas = true,
        )

        assertTrue(items.filterNot { it is SettingsItem.Header }.all { it.icon != null })
        assertTrue(items.filterIsInstance<SettingsItem.Header>().all { it.icon == null })
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
        language = "language",
        languageValue = "language-value",
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
        mapTilt = "map-tilt",
        mapTiltSecondary = "map-tilt-secondary",
        dbStats = "db-stats",
        dbStatsSecondary = "db-stats-secondary",
        imageStats = "image-stats",
        imageStatsSecondary = "image-stats-secondary",
        manageAreas = "manage-areas",
        manageAreasSecondary = "manage-areas-secondary",
        managePlaceImages = "manage-place-images",
        managePlaceImagesSecondary = "manage-place-images-secondary",
        sectionMap = "section-map",
        sectionData = "section-data",
        sectionAdmin = "section-admin",
        sectionGeneral = "section-general",
    )
}
