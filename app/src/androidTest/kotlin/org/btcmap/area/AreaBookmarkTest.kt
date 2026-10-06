package org.btcmap.area

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.R
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.btcmap.settings.authToken
import org.btcmap.ui.AREA_DESCRIPTION_TAG
import org.btcmap.util.assertNoUncaughtException
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AreaBookmarkTest : AreaScreenTest() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun save_whenAuthorized_postsToApiAndPersistsArea() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.user.insert(user(savedAreaIds = emptyList()))
        val posted = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/v4/areas/saved" && request.method == "POST" -> {
                        posted.set(true)
                        jsonResponse("[1]")
                    }

                    path.startsWith("/v4/place-issues") -> jsonResponse(EMPTY_ISSUES_JSON)
                    else -> jsonResponse("[]")
                }
            }
        }

        withArea { _, _ ->
            awaitArea()
            save()

            waitUntil { posted.get() }
            waitUntil { savedAreaIds().contains(1L) }
        }
    }

    @Test
    fun save_whenAlreadySaved_deletesFromApiAndPersistsChange() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.user.insert(user(savedAreaIds = listOf(1)))
        val deleted = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/v4/areas/saved/1" && request.method == "DELETE" -> {
                        deleted.set(true)
                        jsonResponse("[]")
                    }

                    path.startsWith("/v4/place-issues") -> jsonResponse(EMPTY_ISSUES_JSON)
                    else -> jsonResponse("[]")
                }
            }
        }

        withArea { _, _ ->
            awaitArea()
            save()

            waitUntil { deleted.get() }
            waitUntil { savedAreaIds().isEmpty() }
        }
    }

    @Test
    fun save_whenUnauthorized_showsAuthDialogAndDoesNotCallApi() = runBlocking<Unit> {
        databaseRule.db.area.insert(listOf(area()))
        val saving = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.url.encodedPath.startsWith("/v4/areas/saved")) {
                    saving.set(true)
                }
                return areaDispatcher().dispatch(request)
            }
        }

        withArea { _, _ ->
            awaitArea()
            save()

            waitUntil {
                try {
                    onView(withText(R.string.account)).inRoot(isDialog())
                        .check(matches(isDisplayed()))
                    true
                } catch (_: Throwable) {
                    false
                }
            }
            Assert.assertFalse(saving.get())
        }
    }

    @Test
    fun load_whenUserMissingFromDatabase_doesNotLeakUncaughtException() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.area.insert(listOf(area()))
        apiRule.server.dispatcher = areaDispatcher()

        assertNoUncaughtException(
            "Exception escaped the area screen's coroutine to the uncaught handler",
        ) {
            withArea { _, _ ->
                awaitArea()
            }
        }
    }

    @Test
    fun save_whenUserMissingFromDatabase_doesNotLeakUncaughtException() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.area.insert(listOf(area()))
        apiRule.server.dispatcher = areaDispatcher()

        assertNoUncaughtException(
            "Exception escaped the area screen's coroutine to the uncaught handler",
        ) {
            withArea { _, _ ->
                awaitArea()
                save()
            }
        }
    }

    @Test
    fun save_whenApiFails_doesNotLeakUncaughtException() = runBlocking<Unit> {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        databaseRule.db.area.insert(listOf(area()))
        databaseRule.db.user.insert(user(savedAreaIds = emptyList()))
        val attempted = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/v4/areas/saved" && request.method == "POST" -> {
                        attempted.set(true)
                        jsonResponse("""{"message":"boom"}""", code = 500)
                    }

                    path.startsWith("/v4/place-issues") -> jsonResponse(EMPTY_ISSUES_JSON)
                    else -> jsonResponse("[]")
                }
            }
        }

        assertNoUncaughtException(
            "Exception escaped the area screen's coroutine to the uncaught handler",
        ) {
            withArea { _, _ ->
                awaitArea()
                save()

                waitUntil { attempted.get() }
            }
        }
    }

    /** Waits for the area body to render. */
    private fun awaitArea() {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag(AREA_DESCRIPTION_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun save() {
        composeTestRule.onNodeWithContentDescription(
            app.getString(R.string.save),
        ).performClick()
    }

    private fun savedAreaIds(): List<Long> {
        return runBlocking { databaseRule.db.user.select() }
            ?.savedAreas
            ?.map { it.id }
            .orEmpty()
    }

    private fun user(savedAreaIds: List<Long>): User {
        return User(
            id = 1,
            name = "tester",
            roles = emptyList(),
            savedPlaces = emptyList(),
            savedAreas = savedAreaIds.map { SavedItem(id = it, name = "Grand Paris") },
        )
    }
}
