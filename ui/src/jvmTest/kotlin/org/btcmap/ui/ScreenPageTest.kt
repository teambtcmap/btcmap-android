package org.btcmap.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** The shared top-bar chrome the hosts wrap around the plain screens. */
class ScreenPageTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun showsTheTitleContentAndBackAffordance() {
        var back = false

        runComposeUiTest {
            setContent {
                ScreenPage(title = "Settings", onBack = { back = true }) {
                    Text("Body")
                }
            }

            onNodeWithText("Settings").assertIsDisplayed()
            onNodeWithText("Body").assertIsDisplayed()
            onNodeWithContentDescription("Back").performClick()
        }

        assertTrue(back)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersTheActionsSlotAndTheHostsBackLabel() {
        runComposeUiTest {
            setContent {
                ScreenPage(
                    title = "Infra dashboard",
                    onBack = {},
                    backContentDescription = "Navigate up",
                    actions = { Text("Refresh") },
                ) {
                    Text("Body")
                }
            }

            onNodeWithContentDescription("Navigate up").assertIsDisplayed()
            onNodeWithText("Refresh").assertIsDisplayed()
        }
    }
}
