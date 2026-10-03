package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The add-place form. The positioning map is stubbed out (the GPU is not
 * available to the test), so what is driven here is the validation and the
 * draft the screen hands to its submit callback.
 */
class AddPlaceScreenTest {

    private val labels = AddPlaceLabels(
        name = "Name",
        category = "Category",
        address = "Address",
        website = "Website (optional)",
        description = "Description (optional)",
        required = "Required",
        submit = "Submit place",
        submitted = "Place submitted for review.",
        backToMap = "Back to the map",
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun submit_showsRequiredErrorsAndDoesNotSubmit() {
        val drafts = mutableListOf<AddPlaceDraft>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    AddPlaceScreen(
                        lat = 1.0,
                        lon = 2.0,
                        styleUrl = "",
                        styleJson = null,
                        labels = labels,
                        submit = { drafts += it },
                        onBack = {},
                        map = {},
                    )
                }
            }
            onNodeWithTag(ADD_PLACE_SUBMIT_TAG).performScrollTo().performClick()
            onAllNodesWithText("Required").assertCountEquals(3)
        }
        assertTrue(drafts.isEmpty())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filledForm_submitsTheDraftAtTheMapCentre() {
        val drafts = mutableListOf<AddPlaceDraft>()
        runComposeUiTest {
            setContent {
                AppTheme {
                    AddPlaceScreen(
                        lat = 1.5,
                        lon = 2.5,
                        styleUrl = "",
                        styleJson = null,
                        labels = labels,
                        submit = { drafts += it },
                        onBack = {},
                        map = {},
                    )
                }
            }
            onNodeWithTag(ADD_PLACE_NAME_TAG).performTextInput("Cafe")
            onNodeWithTag(ADD_PLACE_CATEGORY_TAG).performTextInput("coffee")
            onNodeWithTag(ADD_PLACE_ADDRESS_TAG).performTextInput("1 Main St")
            onNodeWithTag(ADD_PLACE_SUBMIT_TAG).performScrollTo().performClick()
            onNodeWithText("Place submitted for review.").assertIsDisplayed()
        }
        val draft = drafts.single()
        assertEquals("Cafe", draft.name)
        assertEquals("coffee", draft.category)
        assertEquals("1 Main St", draft.address)
        assertEquals(1.5, draft.lat)
        assertEquals(2.5, draft.lon)
    }
}
