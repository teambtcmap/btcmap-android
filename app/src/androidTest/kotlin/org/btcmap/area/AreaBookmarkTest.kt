package org.btcmap.area

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
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
import org.btcmap.util.assertNoUncaughtException
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AreaBookmarkTest : AreaScreenTest() {

    @Test
    fun save_whenAuthorized_postsToApiAndPersistsArea() {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
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

                    path.endsWith("/events") -> jsonResponse(EMPTY_EVENTS_JSON)
                    path.startsWith("/v4/place-issues") -> jsonResponse(EMPTY_ISSUES_JSON)
                    else -> jsonResponse(areaJson())
                }
            }
        }

        withArea { scenario, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            scenario.onActivity { save(area) }

            waitUntil { posted.get() }
            waitUntil { savedAreaIds().contains(1L) }
        }
    }

    @Test
    fun save_whenAlreadySaved_deletesFromApiAndPersistsChange() {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
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

                    path.endsWith("/events") -> jsonResponse(EMPTY_EVENTS_JSON)
                    path.startsWith("/v4/place-issues") -> jsonResponse(EMPTY_ISSUES_JSON)
                    else -> jsonResponse(areaJson())
                }
            }
        }

        withArea { scenario, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            scenario.onActivity { save(area) }

            waitUntil { deleted.get() }
            waitUntil { savedAreaIds().isEmpty() }
        }
    }

    @Test
    fun save_whenUnauthorized_showsAuthDialogAndDoesNotCallApi() {
        val saving = AtomicBoolean(false)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.url.encodedPath.startsWith("/v4/areas/saved")) {
                    saving.set(true)
                }
                return areaDispatcher().dispatch(request)
            }
        }

        withArea { scenario, area ->
            waitUntilOnMain { !area.requireView().findViewById<View>(R.id.loading).isVisible }

            scenario.onActivity { save(area) }

            waitUntil {
                try {
                    onView(withText(R.string.account)).inRoot(isDialog())
                        .check(matches(isDisplayed()))
                    true
                } catch (t: Throwable) {
                    false
                }
            }
            Assert.assertFalse(saving.get())
        }
    }

    @Test
    fun load_whenUserMissingFromDatabase_doesNotLeakUncaughtException() {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        apiRule.server.dispatcher = areaDispatcher()

        assertNoUncaughtException(
            "Exception escaped the area screen's coroutine to the uncaught handler",
        ) {
            withArea { _, area ->
                waitUntilOnMain {
                    !area.requireView().findViewById<View>(R.id.loading).isVisible
                }
                waitUntilOnMain { area.requireView().findViewById<View>(R.id.content).isVisible }
            }
        }
    }

    @Test
    fun save_whenUserMissingFromDatabase_doesNotLeakUncaughtException() {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
        apiRule.server.dispatcher = areaDispatcher()

        assertNoUncaughtException(
            "Exception escaped the area screen's coroutine to the uncaught handler",
        ) {
            withArea { scenario, area ->
                waitUntilOnMain {
                    !area.requireView().findViewById<View>(R.id.loading).isVisible
                }

                scenario.onActivity { save(area) }
            }
        }
    }

    @Test
    fun save_whenApiFails_doesNotLeakUncaughtException() {
        preferencesRule.prefs.setAuthTokenForTesting("test-token")
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

                    path.endsWith("/events") -> jsonResponse(EMPTY_EVENTS_JSON)
                    path.startsWith("/v4/place-issues") -> jsonResponse(EMPTY_ISSUES_JSON)
                    else -> jsonResponse(areaJson())
                }
            }
        }

        assertNoUncaughtException(
            "Exception escaped the area screen's coroutine to the uncaught handler",
        ) {
            withArea { scenario, area ->
                waitUntilOnMain {
                    !area.requireView().findViewById<View>(R.id.loading).isVisible
                }

                scenario.onActivity { save(area) }

                waitUntil { attempted.get() }
            }
        }
    }

    private fun save(area: AreaFragment) {
        area.requireView().findViewById<Toolbar>(R.id.toolbar)
            .menu.performIdentifierAction(R.id.save, 0)
    }

    private fun savedAreaIds(): List<Long> {
        return databaseRule.db.user.select()
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
