package org.btcmap.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import org.btcmap.ui.map.LocationMove
import org.btcmap.ui.map.MarkerPalette
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The add-event form. The positioning map and the date and time pickers are
 * stubbed out (the test has no GPU and should not drive the Material dialogs),
 * so what is driven here is the validation, the timestamp the picker seam sets
 * and the draft the screen hands to its submit callback.
 */
class AddEventScreenTest {

    private val labels = AddEventLabels(
        title = "Add an event",
        back = "Navigate up",
        name = "Name",
        namePlaceholder = "Phuket Bitcoin Meetup",
        osmId = "OpenStreetMap ID (optional)",
        osmIdPlaceholder = "Search a place or enter node/123",
        osmIdInvalid = "Use node/<id>, way/<id> or relation/<id>",
        website = "Website",
        websitePlaceholder = "example.com",
        startsAt = "Starts at",
        endsAt = "Ends (optional)",
        selectDateTime = "Select date and time",
        clearEnd = "Remove end time",
        dragMap = "Drag the map to set the exact location",
        required = "Required",
        submit = "Submit event",
        submitted = "Event submitted.",
        backToMap = "Back to the map",
        ok = "OK",
        cancel = "Cancel",
        error = ErrorDialogLabels(title = "Error", ok = "OK"),
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
    fun submit_showsNameRequiredAndDoesNotSubmit() {
        val drafts = mutableListOf<AddEventDraft>()
        runComposeUiTest {
            setContent {
                AddEventScreen(
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
                    dateTimePicker = { _, _, _, _ -> },
                )
            }
            onNodeWithTag(ADD_EVENT_SUBMIT_TAG).performScrollTo().performClick()
            onAllNodesWithText("Required").assertCountEquals(2)
        }
        assertTrue(drafts.isEmpty())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failedSubmit_showsTheErrorDialog() {
        runComposeUiTest {
            setContent {
                AddEventScreen(
                    lat = 1.0,
                    lon = 2.0,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    submit = { error("Location (1.0, 2.0) is outside your geofence") },
                    onBack = {},
                    map = { _, _ -> },
                    dateTimePicker = { _, _, onConfirm, _ ->
                        Button(onClick = { onConfirm(LocalDateTime.of(2026, 9, 25, 19, 0)) }) {
                            Text("confirm")
                        }
                    },
                )
            }
            onNodeWithTag(ADD_EVENT_NAME_TAG).performTextInput("Meetup")
            onNodeWithTag(ADD_EVENT_WEBSITE_TAG).performTextInput("https://example.com")
            onNodeWithTag(ADD_EVENT_STARTS_TAG).performClick()
            onNodeWithText("confirm").performClick()
            onNodeWithTag(ADD_EVENT_SUBMIT_TAG).performScrollTo().performClick()

            // The message stays up in a dialog until dismissed, not a snackbar.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Location (1.0, 2.0) is outside your geofence")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithText("OK").performClick()
            onAllNodesWithText("Location (1.0, 2.0) is outside your geofence").assertCountEquals(0)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filledForm_submitsTheDraftAtTheMapCentre() {
        val drafts = mutableListOf<AddEventDraft>()
        runComposeUiTest {
            setContent {
                AddEventScreen(
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
                    // A stubbed picker that confirms a fixed local time instead
                    // of opening the Material dialogs.
                    dateTimePicker = { _, _, onConfirm, _ ->
                        Button(onClick = { onConfirm(LocalDateTime.of(2026, 9, 25, 19, 0)) }) {
                            Text("confirm")
                        }
                    },
                )
            }
            onNodeWithTag(ADD_EVENT_NAME_TAG).performTextInput("Meetup")
            onNodeWithTag(ADD_EVENT_WEBSITE_TAG).performTextInput("https://example.com")
            onNodeWithTag(ADD_EVENT_STARTS_TAG).performClick()
            onNodeWithText("confirm").performClick()
            onNodeWithTag(ADD_EVENT_SUBMIT_TAG).performScrollTo().performClick()
            onNodeWithText("Event submitted.").assertIsDisplayed()
        }
        val draft = drafts.single()
        assertEquals("Meetup", draft.name)
        assertEquals("https://example.com", draft.website)
        assertEquals(1.5, draft.lat)
        assertEquals(2.5, draft.lon)
        assertEquals("2026-09-25T19:00:00", draft.startsAt)
        assertNull(draft.endsAt)
        assertNull(draft.placeOsmId)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun initialOsmId_prefillsTheFieldAndIsSubmittedBack() {
        val drafts = mutableListOf<AddEventDraft>()
        runComposeUiTest {
            setContent {
                AddEventScreen(
                    lat = 1.5,
                    lon = 2.5,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    osmSearch = { emptyList() },
                    initialName = "Meetup",
                    initialWebsite = "https://example.com",
                    initialOsmId = "node/14385962692",
                    submit = { drafts += it },
                    onBack = {},
                    map = { _, _ -> },
                    dateTimePicker = { _, _, onConfirm, _ ->
                        Button(onClick = { onConfirm(LocalDateTime.of(2026, 9, 25, 19, 0)) }) {
                            Text("confirm")
                        }
                    },
                )
            }
            onNodeWithTag(ADD_EVENT_STARTS_TAG).performClick()
            onNodeWithText("confirm").performClick()
            onNodeWithTag(ADD_EVENT_SUBMIT_TAG).performScrollTo().performClick()

            assertEquals("node/14385962692", drafts.single().placeOsmId)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun osmIdField_searchesAndSubmitsThePickedReference() {
        val drafts = mutableListOf<AddEventDraft>()
        val queries = mutableListOf<String>()
        var movedTo: LocationMove? = null
        runComposeUiTest {
            setContent {
                AddEventScreen(
                    lat = 1.5,
                    lon = 2.5,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    osmSearch = { query ->
                        queries += query
                        listOf(
                            OsmPlace(
                                osmId = "node/14385962692",
                                name = "Bitcoin Cafe",
                                lat = 4.0,
                                lon = 5.0,
                            ),
                        )
                    },
                    submit = { drafts += it },
                    onBack = {},
                    map = { moveTo, _ -> movedTo = moveTo },
                    dateTimePicker = { _, _, onConfirm, _ ->
                        Button(onClick = { onConfirm(LocalDateTime.of(2026, 9, 25, 19, 0)) }) {
                            Text("confirm")
                        }
                    },
                )
            }
            onNodeWithTag(ADD_EVENT_NAME_TAG).performTextInput("Meetup")
            onNodeWithTag(ADD_EVENT_OSM_ID_TAG).performTextInput("bit")

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Bitcoin Cafe").fetchSemanticsNodes().isNotEmpty()
            }
            // Only the query with three or more characters was sent.
            assertEquals(listOf("bit"), queries)
            onNodeWithText("Bitcoin Cafe").performClick()
            // Picking the place moves the pin to its coordinates.
            runOnIdle { assertEquals(LocationMove(lat = 4.0, lon = 5.0, sequence = 1), movedTo) }

            onNodeWithTag(ADD_EVENT_WEBSITE_TAG).performTextInput("https://example.com")
            onNodeWithTag(ADD_EVENT_STARTS_TAG).performClick()
            onNodeWithText("confirm").performClick()
            onNodeWithTag(ADD_EVENT_SUBMIT_TAG).performScrollTo().performClick()

            onNodeWithText("Event submitted.").assertIsDisplayed()
        }
        val draft = drafts.single()
        assertEquals("node/14385962692", draft.placeOsmId)
        assertEquals(4.0, draft.lat)
        assertEquals(5.0, draft.lon)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun malformedOsmId_isRejectedBeforeSubmit() {
        val drafts = mutableListOf<AddEventDraft>()
        runComposeUiTest {
            setContent {
                AddEventScreen(
                    lat = 1.5,
                    lon = 2.5,
                    styleUrl = "",
                    styleJson = null,
                    labels = labels,
                    iconFont = null,
                    palette = palette,
                    osmSearch = { emptyList() },
                    submit = { drafts += it },
                    onBack = {},
                    map = { _, _ -> },
                    dateTimePicker = { _, _, _, _ -> },
                )
            }
            onNodeWithTag(ADD_EVENT_NAME_TAG).performTextInput("Meetup")
            onNodeWithTag(ADD_EVENT_OSM_ID_TAG).performTextInput("node/not-a-number")
            onNodeWithTag(ADD_EVENT_WEBSITE_TAG).performTextInput("https://example.com")
            onNodeWithTag(ADD_EVENT_SUBMIT_TAG).performScrollTo().performClick()

            onNodeWithText(labels.osmIdInvalid).assertIsDisplayed()
        }
        assertTrue(drafts.isEmpty())
    }
}
