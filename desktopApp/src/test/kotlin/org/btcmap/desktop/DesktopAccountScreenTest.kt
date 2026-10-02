package org.btcmap.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/** The desktop account page's signed-out state. */
class DesktopAccountScreenTest {

    private val db = testDatabase()
    private val settings = testSettings(db)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun signedOut_showsTheSignInForm() {
        runComposeUiTest {
            setContent {
                DesktopAccountScreen(api = testApi(), db = db, settings = settings, onBack = {})
            }
            onNodeWithText("Username").assertIsDisplayed()
            onNodeWithText("Password").assertIsDisplayed()
            onNodeWithText("Sign in").assertIsNotEnabled()
        }
    }
}
