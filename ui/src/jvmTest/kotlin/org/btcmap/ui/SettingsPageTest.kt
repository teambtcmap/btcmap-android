package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.btcmap.db.table.user.User
import org.btcmap.platform.languageOverride
import org.btcmap.settings.MapStyle
import org.btcmap.settings.language
import org.btcmap.settings.mapBearing
import org.btcmap.settings.mapStyle
import org.btcmap.settings.mapTilt
import org.btcmap.settings.showAttribution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
            onNodeWithText("Allow map tilt").assertIsDisplayed()
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
    fun togglingMapRotation_resetsTheBearing() {
        settings.mapBearing = 45.0
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            // The first toggle is Show attribution, the second Map rotation.
            onAllNodes(isToggleable())[1].performClick()
        }
        assertEquals(0.0, settings.mapBearing)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun togglingMapTilt_resetsTheTilt() {
        settings.mapTilt = 45.0
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            // Show attribution, Map rotation, then Map tilt.
            onAllNodes(isToggleable())[2].performClick()
        }
        assertEquals(0.0, settings.mapTilt)
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
    fun manageAreasRow_isShownToAreaManagers() {
        signIn(roles = listOf("area_manager"))
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            // The account row names the signed-in user once the page has read
            // the cached account, which is also when the admin row is included.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Click to see your profile").fetchSemanticsNodes().isNotEmpty()
            }
            // The list is lazy, so the admin-only rows at the bottom are only
            // composed once scrolled into view.
            onNode(hasScrollAction()).performScrollToNode(hasText("Image cache"))
            onNode(hasScrollAction()).performScrollToNode(hasText("Manage areas"))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manageAreasRow_isHiddenFromRegularUsers() {
        signIn(roles = listOf("user"))
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Click to see your profile").fetchSemanticsNodes().isNotEmpty()
            }
            // Scroll to the last row a regular user has, so an absent admin row
            // is proven absent rather than merely uncomposed.
            onNode(hasScrollAction()).performScrollToNode(hasText("Image cache"))
            onAllNodesWithText("Manage areas").assertCountEquals(0)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manageUsersRow_isShownToAdminsAndOpensTheScreen() {
        signIn(roles = listOf("admin"))
        var opened = false
        runComposeUiTest {
            setContent {
                SettingsPage(
                    settings,
                    db,
                    TEST_SETTINGS_PAGE_LABELS,
                    onOpenAccount = {},
                    onOpenColors = {},
                    onOpenDbStats = {},
                    onOpenManageUsers = { opened = true },
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Click to see your profile").fetchSemanticsNodes().isNotEmpty()
            }
            onNode(hasScrollAction()).performScrollToNode(hasText("Manage users"))
            onNodeWithText("Manage users").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manageUsersRow_isHiddenFromRegularUsers() {
        signIn(roles = listOf("user"))
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Click to see your profile").fetchSemanticsNodes().isNotEmpty()
            }
            onNode(hasScrollAction()).performScrollToNode(hasText("Image cache"))
            onAllNodesWithText("Manage users").assertCountEquals(0)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun managePlaceImagesRow_isShownToAdminsAndOpensTheScreen() {
        signIn(roles = listOf("admin"))
        var opened = false
        runComposeUiTest {
            setContent {
                SettingsPage(
                    settings,
                    db,
                    TEST_SETTINGS_PAGE_LABELS,
                    onOpenAccount = {},
                    onOpenColors = {},
                    onOpenDbStats = {},
                    onOpenManagePlaceImages = { opened = true },
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Click to see your profile").fetchSemanticsNodes().isNotEmpty()
            }
            onNode(hasScrollAction()).performScrollToNode(hasText("Manage place images"))
            onNodeWithText("Manage place images").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun managePlaceImagesRow_isHiddenFromAreaManagersWithoutAnAdminRole() {
        signIn(roles = listOf("area_manager"))
        runComposeUiTest {
            setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Click to see your profile").fetchSemanticsNodes().isNotEmpty()
            }
            // Scroll to the last row this user has, so the absent admin row is
            // proven absent rather than merely uncomposed.
            onNode(hasScrollAction()).performScrollToNode(hasText("Manage areas"))
            onAllNodesWithText("Manage place images").assertCountEquals(0)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun languageRow_picksALanguageAndStoresIt() {
        var changed = false
        try {
            runComposeUiTest {
                setContent {
                    SettingsPage(
                        settings,
                        db,
                        TEST_SETTINGS_PAGE_LABELS,
                        onOpenAccount = {},
                        onOpenColors = {},
                        onOpenDbStats = {},
                        onLanguageChanged = { changed = true },
                    )
                }
                onNodeWithText("Language").performClick()
                onNodeWithText("Deutsch").performClick()
            }
            assertEquals("de", settings.language)
            assertTrue(changed)
        } finally {
            languageOverride = null
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun languageRow_canFollowTheSystemAgain() {
        settings.language = "de"
        try {
            runComposeUiTest {
                setContent { SettingsPage(settings, db, TEST_SETTINGS_PAGE_LABELS, onOpenAccount = {}, onOpenColors = {}, onOpenDbStats = {}) }
                // The row shows the stored language, so this opens the picker.
                onNodeWithText("Deutsch").performClick()
                onNodeWithText("System default").performClick()
            }
            assertNull(settings.language)
        } finally {
            languageOverride = null
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
