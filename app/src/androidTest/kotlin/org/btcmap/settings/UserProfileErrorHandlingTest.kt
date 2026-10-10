package org.btcmap.settings

import org.btcmap.i18n.Strings

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import org.btcmap.nav.AppRootFragment
import org.btcmap.ui.AppRoute
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.btcmap.util.AppTestCase
import org.btcmap.util.assertNoUncaughtException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileErrorHandlingTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun deleteSavedPlace_whenRefreshFails_doesNotLeakUncaughtException() = runBlocking<Unit> {
        val driver = FailingDriver()
        val db = Database(driver, ":memory:")
        db.user.insert(user())
        app.dbForTesting = db

        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = if (request.url.encodedPath == "/v4/users/me") {
                    """{"id":1,"name":"tester","roles":[],"saved_places":[],"saved_areas":[]}"""
                } else {
                    "[]"
                }
                return MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body(body)
                    .build()
            }
        }

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, AppRootFragment.create(AppRoute.UserProfile), PROFILE_TAG)
                    }
                }

                composeTestRule.waitUntil(5_000) {
                    composeTestRule
                        .onAllNodesWithText(Strings.current()["saved_places"])
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeTestRule
                    .onAllNodesWithText(Strings.current()["saved_places"])
                    .onFirst()
                    .performClick()

                composeTestRule.waitUntil(5_000) {
                    composeTestRule.onAllNodesWithText("Test Place").fetchSemanticsNodes().isNotEmpty()
                }

                assertNoUncaughtException(
                    "Exception escaped the user profile screen's coroutine to the uncaught handler",
                ) {
                    scenario.onActivity { driver.failing = true }
                    composeTestRule
                        .onAllNodesWithContentDescription(Strings.current()["delete"])
                        .onFirst()
                        .performClick()
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun user(): User {
        return User(
            id = 1,
            name = "tester",
            roles = emptyList(),
            savedPlaces = listOf(SavedItem(id = 1L, name = "Test Place")),
            savedAreas = emptyList(),
        )
    }

    private class FailingDriver : SQLiteDriver {
        @Volatile
        var failing = false

        private val delegate = AndroidSQLiteDriver()

        override fun open(path: String): SQLiteConnection {
            val connection = delegate.open(path)
            return object : SQLiteConnection {
                override fun prepare(sql: String): SQLiteStatement {
                    if (failing) throw RuntimeException()
                    return connection.prepare(sql)
                }

                override fun close() {
                    connection.close()
                }
            }
        }
    }

    companion object {
        private const val PROFILE_TAG = "profile"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
