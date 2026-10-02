package org.btcmap.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.btcmap.settings.showAttribution
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The desktop settings screen. The Account row opened nothing for a while
 * because the window called the screen without its callback; the screen now
 * requires the callback, and this drives the row so the wiring cannot regress.
 */
class DesktopSettingsScreenTest {

    private val db = testDatabase()
    private val settings = testSettings(db)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun accountRow_opensTheAccountPage() {
        var opened = false
        runComposeUiTest {
            setContent {
                DesktopSettingsScreen(settings, onOpenAccount = { opened = true })
            }
            onNodeWithText("Account").performClick()
        }
        assertTrue(opened)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rows_renderTheStoredValues() {
        runComposeUiTest {
            setContent { DesktopSettingsScreen(settings, onOpenAccount = {}) }
            onNodeWithText("Show attribution").assertIsDisplayed()
            onNodeWithText("Map rotation").assertIsDisplayed()
            onNodeWithText("Account").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun togglingAttribution_writesItThrough() {
        assertTrue(settings.showAttribution)
        runComposeUiTest {
            setContent { DesktopSettingsScreen(settings, onOpenAccount = {}) }
            // Only the switch is clickable; the row is not (toggles have no
            // action of their own).
            onAllNodes(isToggleable())[0].performClick()
        }
        assertFalse(settings.showAttribution)
    }
}
