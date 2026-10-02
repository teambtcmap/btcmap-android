package org.btcmap.desktop

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
import org.btcmap.ui.AppTheme

/**
 * The desktop profile over a cached account: the saved lists render, and logging
 * out clears the session and tells the host.
 */
class DesktopProfileTest {

    private val db = testDatabase()
    private val settings = testSettings(db)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersSavedItemsAndLogsOut() {
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

        var loggedOut = false
        runComposeUiTest {
            setContent {
                AppTheme {
                    DesktopProfile(
                        api = testApi(),
                        db = db,
                        settings = settings,
                        onLoggedOut = { loggedOut = true },
                    )
                }
            }
            // The account is read off the main thread, so wait for it to land.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("alice").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Cafe").assertExists()
            onNodeWithText("Warsaw").assertExists()
            // No stored token, so the log-out clears the session locally and
            // never reaches the network.
            onNodeWithText("Log out").performScrollTo().performClick()
            waitUntil(timeoutMillis = 5_000) { loggedOut }
        }

        assertTrue(loggedOut)
        assertNull(runBlocking { db.user.select() })
    }
}
