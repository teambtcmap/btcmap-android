package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.btcmap.settings.MapStyle
import org.btcmap.settings.mapStyle
import org.btcmap.settings.showAttribution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The settings page: it renders the shared row list Android does, so these tests
 * pin the rows and the wiring of the ones that open another screen or a dialog.
 */
class SettingsPageTest {

    private val db = testDatabase()
    private val settings = testSettings(db)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rows_renderTheSameSettingsAsAndroid() {
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, {}, {}, {}) }
            onNodeWithText("Map style").assertIsDisplayed()
            onNodeWithText("Customize colors").assertIsDisplayed()
            onNodeWithText("Only show places").assertIsDisplayed()
            onNodeWithText("Show attribution").assertIsDisplayed()
            onNodeWithText("Allow map rotation").assertIsDisplayed()
            onNodeWithText("Database").assertIsDisplayed()
            onNodeWithText("Account").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun accountRow_opensTheAccountPage() {
        var opened = false
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, { opened = true }, {}, {}) }
            onNodeWithText("Account").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun customizeColorsRow_opensTheColorsPage() {
        var opened = false
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, {}, { opened = true }, {}) }
            onNodeWithText("Customize colors").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun databaseRow_opensTheDatabasePage() {
        var opened = false
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, {}, {}, { opened = true }) }
            onNodeWithText("Database").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mapStyleRow_picksAStyleAndStoresIt() {
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, {}, {}, {}) }
            onNodeWithText("Map style").performClick()
            onNodeWithText("OpenFreeMap Dark").performClick()
        }
        assertEquals(MapStyle.Dark, settings.mapStyle)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun togglingAttribution_writesItThrough() {
        assertTrue(settings.showAttribution)
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, {}, {}, {}) }
            // The first toggle is Show attribution, the second Map rotation.
            onAllNodes(isToggleable())[0].performClick()
        }
        assertFalse(settings.showAttribution)
    }
}
