package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.btcmap.db.table.user.User
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
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
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
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = { opened = true }, onOpenColors = {}, onOpenDbStats = {}) }
            onNodeWithText("Account").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun customizeColorsRow_opensTheColorsPage() {
        var opened = false
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = { opened = true }, onOpenDbStats = {}) }
            onNodeWithText("Customize colors").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun databaseRow_opensTheDatabasePage() {
        var opened = false
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = { opened = true }) }
            onNodeWithText("Database").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mapStyleRow_picksAStyleAndStoresIt() {
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
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
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            // The first toggle is Show attribution, the second Map rotation.
            onAllNodes(isToggleable())[0].performClick()
        }
        assertFalse(settings.showAttribution)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun togglingAttribution_fromTheLabel_writesItThrough() {
        assertTrue(settings.showAttribution)
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            // The whole row is the toggle, so the label is a target too.
            onNodeWithText("Show attribution").performClick()
        }
        assertFalse(settings.showAttribution)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rows_areGroupedUnderHeaders() {
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            onNodeWithText("Map").assertIsDisplayed()
            onNodeWithText("Data").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manageAreasRow_isShownToAreaAdmins() {
        signIn(roles = listOf("area_admin"))
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Manage areas").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manageAreasRow_isHiddenFromRegularUsers() {
        signIn(roles = listOf("user"))
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            waitForIdle()
            onAllNodesWithText("Manage areas").assertCountEquals(0)
        }
    }

    private fun signIn(roles: List<String>) = runBlocking {
        settings.setAuthTokenForTesting("token")
        db.user.insert(
            User(
                id = 1L,
                name = "tester",
                roles = roles,
                savedPlaces = emptyList(),
                savedAreas = emptyList(),
            ),
        )
    }
}
