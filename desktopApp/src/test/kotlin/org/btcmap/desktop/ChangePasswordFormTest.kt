package org.btcmap.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.btcmap.ui.AppTheme

/** The change-password form's client-side validation and saved credentials. */
class ChangePasswordFormTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyForm_showsErrorsAndDoesNotSave() {
        var saved = false
        runComposeUiTest {
            setContent {
                AppTheme {
                    ChangePasswordForm(onCancel = {}, save = { _, _ -> saved = true })
                }
            }
            onNodeWithTag(PROFILE_PASSWORD_SAVE_TAG).performClick()
            // The current and the new password are both required.
            onAllNodesWithText("Required").assertCountEquals(2)
        }
        assertFalse(saved)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun validPasswords_save() {
        var saved: Pair<String, String>? = null
        runComposeUiTest {
            setContent {
                AppTheme {
                    ChangePasswordForm(onCancel = {}, save = { current, new -> saved = current to new })
                }
            }
            onNodeWithTag(PROFILE_PASSWORD_CURRENT_TAG).performTextInput("old")
            onNodeWithTag(PROFILE_PASSWORD_NEW_TAG).performTextInput("newpassword")
            onNodeWithTag(PROFILE_PASSWORD_CONFIRM_TAG).performTextInput("newpassword")
            onNodeWithTag(PROFILE_PASSWORD_SAVE_TAG).performClick()
        }
        assertEquals("old" to "newpassword", saved)
    }
}
