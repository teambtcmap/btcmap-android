package org.btcmap.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.btcmap.ui.AppTheme

/** The change-username form: the name is required and saved trimmed. */
class ChangeUsernameFormTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun blankName_showsRequiredAndDoesNotSave() {
        var saved: String? = null
        runComposeUiTest {
            setContent {
                AppTheme {
                    ChangeUsernameForm(currentName = "", onCancel = {}, save = { saved = it })
                }
            }
            onNodeWithTag(PROFILE_USERNAME_SAVE_TAG).performClick()
            onNodeWithText("Required").assertExists()
        }
        assertNull(saved)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filledName_savesTrimmed() {
        var saved: String? = null
        runComposeUiTest {
            setContent {
                AppTheme {
                    ChangeUsernameForm(currentName = "", onCancel = {}, save = { saved = it })
                }
            }
            onNodeWithTag(PROFILE_USERNAME_FIELD_TAG).performTextInput(" bob ")
            onNodeWithTag(PROFILE_USERNAME_SAVE_TAG).performClick()
        }
        assertEquals("bob", saved)
    }
}
