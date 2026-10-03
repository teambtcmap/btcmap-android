package org.btcmap.auth

import kotlinx.coroutines.runBlocking
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.ui.AUTH_FIELD_TAG_PREFIX
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInErrorTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun signIn_whenFailureHasNoMessage_showsSignInErrorMessage() = runBlocking<Unit> {
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = if (request.url.encodedPath == "/v4/users/satoshi/tokens") {
                    """
                    {
                        "token": "token-1",
                        "user": {
                            "id": 1,
                            "name": "satoshi",
                            "roles": ["user"],
                            "saved_places": [],
                            "saved_areas": []
                        }
                    }
                    """.trimIndent()
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

        val driver = FailingDriver()
        val db = Database(driver, ":memory:")
        app.dbForTesting = db

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val host = AuthHostFragment()
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, host, HOST_TAG)
                    }
                    host.showAuthDialog()
                }

                composeTestRule
                    .onNodeWithText(app.getString(R.string.log_in_with_existing_account))
                    .performClick()
                composeTestRule
                    .onNodeWithTag(AUTH_FIELD_TAG_PREFIX + "username")
                    .performTextInput("satoshi")
                composeTestRule
                    .onNodeWithTag(AUTH_FIELD_TAG_PREFIX + "password")
                    .performTextInput("hunter2")

                scenario.onActivity { driver.failing = true }

                onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())

                waitUntil {
                    try {
                        onView(withText(R.string.failed_to_sign_in)).inRoot(isDialog())
                            .check(matches(isDisplayed()))
                        true
                    } catch (t: Throwable) {
                        false
                    }
                }

                onView(withText(R.string.failed_to_sign_in)).inRoot(isDialog())
                    .check(matches(isDisplayed()))
            }
        } finally {
            app.apiForTesting = null
            app.dbForTesting = null
            app.mapStyleUriForTesting = null
        }
    }

    class AuthHostFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            return FrameLayout(requireContext())
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            registerAuthResultListener { }
        }
    }

    private class FailingDriver : SQLiteDriver {
        @Volatile
        var failing = false

        private val delegate = AndroidSQLiteDriver()

        override fun open(fileName: String): SQLiteConnection {
            val connection = delegate.open(fileName)
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
        private const val HOST_TAG = "auth-host"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
