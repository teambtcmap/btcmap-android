package org.btcmap.feed

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.btcmap.settings.authToken
import org.btcmap.util.AppTestCase
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Saved tab's empty state: signed out, signed in with nothing saved and
 * signed in with saved items but no activity are three different messages, and
 * the first two must not hit the activity endpoint.
 */
@RunWith(AndroidJUnit4::class)
class SavedActivityTabTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun signedOut_showsSignInMessageWithoutCallingApi() = runBlocking<Unit> {
        val apiCalls = AtomicInteger(0)
        apiRule.server.dispatcher = countingDispatcher(apiCalls)

        withSavedTab { _, _ ->
            assertShowsMessage(R.string.activity_empty_saved_signed_out)
            Assert.assertEquals(0, apiCalls.get())
        }
    }

    @Test
    fun signedInWithNoSavedItems_showsNoItemsMessageWithoutCallingApi() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.user.insert(user())
        val apiCalls = AtomicInteger(0)
        apiRule.server.dispatcher = countingDispatcher(apiCalls)

        withSavedTab { _, _ ->
            assertShowsMessage(R.string.activity_empty_saved_no_items)
            Assert.assertEquals(0, apiCalls.get())
        }
    }

    @Test
    fun signedInWithSavedItemsAndNoActivity_showsNoActivityMessage() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.user.insert(user(savedPlaceIds = listOf(1)))
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = jsonResponse()
        }

        withSavedTab { _, _ ->
            assertShowsMessage(R.string.activity_empty_saved_no_activity)
        }
    }

    /** Waits for the given empty message to be rendered, then asserts it. */
    private fun assertShowsMessage(resId: Int) {
        val message = app.getString(resId)
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(message).assertExists()
    }

    private fun withSavedTab(
        block: (ActivityScenario<Activity>, SavedActivityFragment) -> Unit,
    ) {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var tab: SavedActivityFragment
                scenario.onActivity { activity ->
                    tab = SavedActivityFragment()
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, tab, TAB_TAG)
                    }
                }
                block(scenario, tab)
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun countingDispatcher(calls: AtomicInteger) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            calls.incrementAndGet()
            return jsonResponse()
        }
    }

    private fun user(savedPlaceIds: List<Long> = emptyList()): User {
        return User(
            id = 1,
            name = "tester",
            roles = emptyList(),
            savedPlaces = savedPlaceIds.map { SavedItem(id = it, name = "Test Place") },
            savedAreas = emptyList(),
        )
    }

    private fun jsonResponse(body: String = "[]"): MockResponse {
        return MockResponse.Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
    }

    private companion object {
        const val TAB_TAG = "saved-tab"
        const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
