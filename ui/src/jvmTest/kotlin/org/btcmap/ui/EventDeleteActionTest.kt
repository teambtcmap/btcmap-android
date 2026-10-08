package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The event screen's top-bar delete action: hidden when the user may not delete,
 * and otherwise a confirmation that runs the delete and leaves the screen, or
 * keeps the dialog open with an error when the call fails.
 */
class EventDeleteActionTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun withoutDeleteCallback_noActionIsDrawn() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    EventDeleteAction(
                        labels = TEST_EVENT_SCREEN_LABELS,
                        onDelete = null,
                        onDeleted = {},
                    )
                }
            }
            onNodeWithTag(EVENT_DELETE_TAG).assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun delete_confirmsThenRunsAndLeavesTheScreen() {
        var deleted = 0
        var left = 0
        runComposeUiTest {
            setContent {
                AppTheme {
                    EventDeleteAction(
                        labels = TEST_EVENT_SCREEN_LABELS,
                        onDelete = { deleted++ },
                        onDeleted = { left++ },
                    )
                }
            }
            onNodeWithTag(EVENT_DELETE_TAG).assertIsDisplayed()
            onNodeWithTag(EVENT_DELETE_TAG).performClick()
            onNodeWithText("Delete this event?").assertIsDisplayed()
            onNodeWithTag(EVENT_DELETE_CONFIRM_TAG).performClick()
            waitForIdle()
        }
        assertEquals(1, deleted)
        assertEquals(1, left)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deleteFailure_keepsTheDialogWithTheError() {
        var left = 0
        runComposeUiTest {
            setContent {
                AppTheme {
                    EventDeleteAction(
                        labels = TEST_EVENT_SCREEN_LABELS,
                        onDelete = { throw RuntimeException("403") },
                        onDeleted = { left++ },
                    )
                }
            }
            onNodeWithTag(EVENT_DELETE_TAG).performClick()
            onNodeWithTag(EVENT_DELETE_CONFIRM_TAG).performClick()
            waitForIdle()
            onNodeWithTag(EVENT_DELETE_ERROR_TAG).assertIsDisplayed()
            onNodeWithText("Couldn't delete the event").assertIsDisplayed()
        }
        assertEquals(0, left)
    }
}
