package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * The map's note sheet: it carries the note's text plus its visibility and
 * creation date, so a tapped note read as a panel rather than a dialog.
 */
@OptIn(ExperimentalTestApi::class)
class NoteSheetTest {

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
                        labels = NoteSheetLabels(
                            title = "Note",
                            public = "Public",
                            private = "Private",
                            created = { "Created $it" },
                        ),
                        onDismiss = {},
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
                        labels = NoteSheetLabels(
                            title = "Note",
                            public = "Public",
                            private = "Private",
                            created = { "Created $it" },
                        ),
                        onDismiss = {},
                    )
                }
            }
            onNodeWithText("Private").assertIsDisplayed()
        }
    }
}
