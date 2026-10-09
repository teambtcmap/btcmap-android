package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * The map's note sheet: it carries the note's text plus its visibility and
 * creation date, and edit and delete actions, so a tapped note reads as a panel
 * rather than a dialog.
 */
@OptIn(ExperimentalTestApi::class)
class NoteSheetTest {

    private val labels = NoteSheetLabels(
        title = "Note",
        public = "Public",
        private = "Private",
        created = { "Created $it" },
        edit = "Edit note",
        editTitle = "Edit note",
        editFailed = "Couldn't save the note",
        save = "Save",
        delete = "Delete",
        deleteConfirmTitle = "Delete this note?",
        deleteConfirmMessage = "This note will be permanently removed.",
        deleteFailed = "Couldn't delete the note",
        cancel = "Cancel",
    )

    @Test
    fun showsTheTextVisibilityAndCreation() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    NoteSheet(
                        text = "ATM is inside, ask at the bar",
                        icon = "local_atm",
                        public = true,
                        createdAt = Instant.parse("2026-01-02T10:00:00Z"),
                        labels = labels,
                        onDismiss = {},
                        onEditText = {},
                        onDelete = {},
                    )
                }
            }
            onNodeWithText("Note").assertIsDisplayed()
            onNodeWithTag(NOTE_SHEET_TEXT_TAG).assertIsDisplayed()
            onNodeWithText("ATM is inside, ask at the bar").assertIsDisplayed()
            onNodeWithTag(NOTE_SHEET_VISIBILITY_TAG).assertIsDisplayed()
            onNodeWithText("Public").assertIsDisplayed()
            onNodeWithTag(NOTE_SHEET_CREATED_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun showsAPrivateNoteAsPrivate() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    NoteSheet(
                        text = "Don't tell anyone",
                        icon = "lock",
                        public = false,
                        createdAt = Instant.parse("2026-01-02T10:00:00Z"),
                        labels = labels,
                        onDismiss = {},
                        onEditText = {},
                        onDelete = {},
                    )
                }
            }
            onNodeWithText("Private").assertIsDisplayed()
        }
    }

    @Test
    fun withoutEditCallback_noPencilIsDrawn() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    NoteSheet(
                        text = "ATM is inside",
                        icon = "local_atm",
                        public = true,
                        createdAt = Instant.parse("2026-01-02T10:00:00Z"),
                        labels = labels,
                        onDismiss = {},
                        onEditText = null,
                    )
                }
            }
            onNodeWithTag(NOTE_EDIT_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun edit_savesTheNewText() {
        var saved: String? = null
        runComposeUiTest {
            setContent {
                AppTheme {
                    NoteSheet(
                        text = "old text",
                        icon = "local_atm",
                        public = true,
                        createdAt = Instant.parse("2026-01-02T10:00:00Z"),
                        labels = labels,
                        onDismiss = {},
                        onEditText = { saved = it },
                    )
                }
            }
            onNodeWithTag(NOTE_EDIT_TAG).performClick()
            onNodeWithText("Edit note").assertIsDisplayed()
            onNodeWithTag(NOTE_EDIT_FIELD_TAG).performTextReplacement("new text")
            onNodeWithTag(NOTE_EDIT_SAVE_TAG).performClick()
            waitForIdle()
        }
        assertEquals("new text", saved)
    }

    @Test
    fun withoutDeleteCallback_noActionIsDrawn() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    NoteSheet(
                        text = "ATM is inside",
                        icon = "local_atm",
                        public = true,
                        createdAt = Instant.parse("2026-01-02T10:00:00Z"),
                        labels = labels,
                        onDismiss = {},
                        onDelete = null,
                    )
                }
            }
            onNodeWithTag(NOTE_DELETE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun delete_confirmsThenRunsAndDismisses() {
        var deleted = 0
        var dismissed = 0
        runComposeUiTest {
            setContent {
                AppTheme {
                    NoteSheet(
                        text = "ATM is inside, ask at the bar",
                        icon = "local_atm",
                        public = true,
                        createdAt = Instant.parse("2026-01-02T10:00:00Z"),
                        labels = labels,
                        onDismiss = {},
                        onDelete = { deleted++ },
                        onDeleted = { dismissed++ },
                    )
                }
            }
            onNodeWithTag(NOTE_DELETE_TAG).assertIsDisplayed()
            onNodeWithTag(NOTE_DELETE_TAG).performClick()
            onNodeWithText("Delete this note?").assertIsDisplayed()
            onNodeWithTag(NOTE_DELETE_CONFIRM_TAG).performClick()
            waitForIdle()
        }
        assertEquals(1, deleted)
        assertEquals(1, dismissed)
    }
}
