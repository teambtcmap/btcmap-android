package org.btcmap.ui

import androidx.compose.ui.graphics.Color
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
import org.btcmap.ui.map.MarkerPalette
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
        title = "Add a place",
        back = "Navigate up",
        name = "Name",
        namePlaceholder = "Satoshi Cafe",
        category = "Category",
        categoryPlaceholder = "cafe",
        address = "Address",
        addressPlaceholder = "1 HODL Lane, Block 21",
        website = "Website (optional)",
        websitePlaceholder = "example.com",
        description = "Description (optional)",
        descriptionPlaceholder = "Accepts Lightning payments via Blink wallet, ATM in the back room",
        dragMap = "Drag the map to set the exact location",
        required = "Required",
        submit = "Submit place",
        submitted = "Place submitted for review.",
        backToMap = "Back to the map",
    )

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
    fun submit_showsRequiredErrorsAndDoesNotSubmit() {
        val drafts = mutableListOf<AddPlaceDraft>()
        runComposeUiTest {
            setContent {
                AddPlaceScreen(
                    lat = 1.0,
                    lon = 2.0,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    submit = { drafts += it },
                    onBack = {},
                    map = {},
                )
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
                AddPlaceScreen(
                    lat = 1.5,
                    lon = 2.5,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    submit = { drafts += it },
                    onBack = {},
                    map = {},
                )
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun prefilledFromABasemapPoi_keepsTheSeededNameAndCategory() {
        val drafts = mutableListOf<AddPlaceDraft>()
        runComposeUiTest {
            setContent {
                AddPlaceScreen(
                    lat = 39.9,
                    lon = 116.4,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    submit = { drafts += it },
                    onBack = {},
                    initialName = "Beijing Cafe",
                    initialCategory = "cafe",
                    map = {},
                )
            }
            onNodeWithText("Beijing Cafe").assertIsDisplayed()
            onNodeWithText("cafe").assertIsDisplayed()
            // The seeded fields still leave the required address to fill in.
            onNodeWithTag(ADD_PLACE_ADDRESS_TAG).performTextInput("1 Main St")
            onNodeWithTag(ADD_PLACE_SUBMIT_TAG).performScrollTo().performClick()
        }
        val draft = drafts.single()
        assertEquals("Beijing Cafe", draft.name)
        assertEquals("cafe", draft.category)
        assertEquals(39.9, draft.lat)
        assertEquals(116.4, draft.lon)
    }
}
