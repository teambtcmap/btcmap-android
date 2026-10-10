package org.btcmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User

/**
 * The profile page over a cached account: opening a saved list, and logging out
 * clearing the session and telling the host.
 */
class ProfileScreenTest {

    private val db = testDatabase()
    private val settings = testSettings(db)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun opensSavedPlacesSubPage() {
        seedUser()

        runComposeUiTest {
            setContent { TestProfile() }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("alice").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Saved places").performScrollTo().performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Cafe").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun logsOut() {
        seedUser()

        var loggedOut = false
        runComposeUiTest {
            setContent { TestProfile(onLoggedOut = { loggedOut = true }) }
            // The account is read off the main thread, so wait for it to land.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("alice").fetchSemanticsNodes().isNotEmpty()
            }
            // No stored token, so the log-out clears the session locally and
            // never reaches the network.
            onNodeWithText("Log out").performScrollTo().performClick()
            waitUntil(timeoutMillis = 5_000) { loggedOut }
        }

        assertTrue(loggedOut)
        assertNull(runBlocking { db.user.select() })
    }

    private fun seedUser() {
        runBlocking {
            db.user.insert(
                User(
                    id = 1L,
                    name = "alice",
                    roles = emptyList(),
                    savedPlaces = listOf(SavedItem(10L, "Cafe")),
                    savedAreas = listOf(SavedItem(20L, "Warsaw")),
                ),
            )
        }
    }

    @Composable
    private fun TestProfile(onLoggedOut: () -> Unit = {}) {
        var showUploadedImages by remember { mutableStateOf(false) }
        var showMyEvents by remember { mutableStateOf(false) }
        var showMyNotes by remember { mutableStateOf(false) }
        var showSavedPlaces by remember { mutableStateOf(false) }
        var showSavedAreas by remember { mutableStateOf(false) }
        AppTheme {
            ProfileScreen(
                api = testApi(),
                db = db,
                settings = settings,
                profileLabels = TEST_PROFILE_LABELS,
                formLabels = TEST_PROFILE_FORM_LABELS,
                imagesLabels = TEST_UPLOADED_IMAGES_LABELS,
                eventsLabels = TEST_MY_EVENTS_LABELS,
                notesLabels = TEST_MY_NOTES_LABELS,
                savedPlacesLabels = TEST_SAVED_PLACES_LABELS,
                savedAreasLabels = TEST_SAVED_AREAS_LABELS,
                mapStyleUrl = "",
                mapStyleJson = null,
                showUploadedImages = showUploadedImages,
                onShowUploadedImagesChange = { showUploadedImages = it },
                showMyEvents = showMyEvents,
                onShowMyEventsChange = { showMyEvents = it },
                showMyNotes = showMyNotes,
                onShowMyNotesChange = { showMyNotes = it },
                showSavedPlaces = showSavedPlaces,
                onShowSavedPlacesChange = { showSavedPlaces = it },
                showSavedAreas = showSavedAreas,
                onShowSavedAreasChange = { showSavedAreas = it },
                onLoggedOut = onLoggedOut,
            )
        }
    }
}
