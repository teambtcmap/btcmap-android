package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The my-notes management list: a note's visibility is toggled through the API
 * callback and a deleted note drops its row.
 */
class MyNotesScreenTest {

    private val labels = TEST_MY_NOTES_LABELS

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun toggleVisibility_updatesTheNote() {
        val updates = mutableListOf<Pair<Long, Boolean>>()
        runComposeUiTest {
            setContent {
                MyNotesScreen(
                    labels = labels,
                    load = { listOf(MyNoteUi(id = 1L, text = "ATM is inside", public = false, lat = 1.0, lon = 2.0)) },
                    update = { id, public -> updates += id to public },
                    delete = {},
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("ATM is inside").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(MY_NOTES_PUBLIC_TAG_PREFIX + 1L).performClick()
            waitUntil(timeoutMillis = 5_000) { updates.isNotEmpty() }
        }
        assertEquals(listOf(1L to true), updates)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun delete_removesTheRow() {
        val deleted = mutableListOf<Long>()
        runComposeUiTest {
            setContent {
                MyNotesScreen(
                    labels = labels,
                    load = { listOf(MyNoteUi(id = 3L, text = "Reminder", public = true, lat = 1.0, lon = 2.0)) },
                    update = { _, _ -> },
                    delete = { deleted += it.id },
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Reminder").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(MY_NOTES_DELETE_TAG_PREFIX + 3L).performClick()
            waitUntil(timeoutMillis = 5_000) { deleted.isNotEmpty() }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Reminder").fetchSemanticsNodes().isEmpty()
            }
        }
        assertEquals(listOf(3L), deleted)
    }
}
