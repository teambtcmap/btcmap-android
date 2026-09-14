package org.btcmap.settings

import android.view.View
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.db.table.user.User
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.assertNoUncaughtException
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileErrorHandlingTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun deleteSavedPlace_whenRefreshFails_doesNotLeakUncaughtException() {
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
                lateinit var profile: UserProfileFragment
                scenario.onActivity { activity ->
                    profile = UserProfileFragment()
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, profile, PROFILE_TAG)
                    }
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()

                assertNoUncaughtException(
                    "Exception escaped the user profile screen's coroutine to the uncaught handler",
                ) {
                    scenario.onActivity {
                        driver.failing = true
                        val list = profile.requireView()
                            .findViewById<RecyclerView>(R.id.savedPlacesList)
                        val delete = list.getChildAt(0).findViewById<View>(R.id.deleteButton)
                        Assert.assertNotNull(delete)
                        delete.performClick()
                    }
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun user(): User {
        val place = JsonObject().apply {
            addProperty("id", 1L)
            addProperty("name", "Test Place")
        }
        return User(
            id = 1,
            name = "tester",
            roles = JsonArray(),
            savedPlaces = JsonArray().apply { add(place) },
            savedAreas = JsonArray(),
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
