package org.btcmap.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.btcmap.ui.map.MarkerPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The add-note form. The positioning map is stubbed out (the GPU is not
 * available to the test), so what is driven here is the validation and the
 * draft the screen hands to its submit callback.
 */
class AddNoteScreenTest {

    private val labels = TEST_ADD_NOTE_LABELS

    private val palette = MarkerPalette(
        markerBackground = Color(0xFF00BCD4),
        markerIcon = Color.White,
        boostedMarkerBackground = Color(0xFF00BCD4),
        boostedMarkerIcon = Color.White,
        badgeBackground = Color(0xFFE53935),
        badgeText = Color.White,
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun blankNote_showsRequiredAndDoesNotSubmit() {
        val drafts = mutableListOf<AddNoteDraft>()
        runComposeUiTest {
            setContent {
                AddNoteScreen(
                    lat = 1.0,
                    lon = 2.0,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    submit = { drafts += it },
                    onBack = {},
                    map = { _, _ -> },
                )
            }
            onNodeWithTag(ADD_NOTE_SUBMIT_TAG).performScrollTo().performClick()
            onNodeWithText("Required").assertExists()
        }
        assertTrue(drafts.isEmpty())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filledNote_submitsDraftAtTheMapCentreAndVisibility() {
        val drafts = mutableListOf<AddNoteDraft>()
        runComposeUiTest {
            setContent {
                AddNoteScreen(
                    lat = 1.5,
                    lon = 2.5,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    submit = { drafts += it },
                    onBack = {},
                    map = { _, _ -> },
                )
            }
            onNodeWithTag(ADD_NOTE_TEXT_TAG).performTextInput("ATM is inside")
            onNodeWithTag(ADD_NOTE_PUBLIC_TAG).performScrollTo().performClick()
            onNodeWithTag(ADD_NOTE_SUBMIT_TAG).performScrollTo().performClick()
            onNodeWithText("Note added").assertIsDisplayed()
        }
        val draft = drafts.single()
        assertEquals("ATM is inside", draft.text)
        assertTrue(draft.public)
        assertEquals(DEFAULT_NOTE_ICON, draft.icon)
        assertEquals(1.5, draft.lat)
        assertEquals(2.5, draft.lon)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun choosingAnIcon_submitsThatIcon() {
        val drafts = mutableListOf<AddNoteDraft>()
        runComposeUiTest {
            setContent {
                AddNoteScreen(
                    lat = 1.0,
                    lon = 2.0,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    submit = { drafts += it },
                    onBack = {},
                    map = { _, _ -> },
                )
            }
            onNodeWithTag(ADD_NOTE_TEXT_TAG).performTextInput("ATM is inside")
            onNodeWithTag(ADD_NOTE_ICON_TAG_PREFIX + "star").performScrollTo().performClick()
            onNodeWithTag(ADD_NOTE_SUBMIT_TAG).performScrollTo().performClick()
        }
        assertEquals("star", drafts.single().icon)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun visibilityCaption_followsTheSelectedSegment() {
        runComposeUiTest {
            setContent {
                AddNoteScreen(
                    lat = 1.0,
                    lon = 2.0,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    submit = {},
                    onBack = {},
                    map = { _, _ -> },
                )
            }
            // Private is the default.
            onNodeWithText("Only you can see your private notes").assertExists()

            onNodeWithTag(ADD_NOTE_PUBLIC_TAG).performScrollTo().performClick()

            onNodeWithText("Public notes can be seen by anyone").assertExists()
        }
    }
}
