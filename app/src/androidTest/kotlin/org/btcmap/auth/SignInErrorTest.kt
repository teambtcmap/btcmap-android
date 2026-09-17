package org.btcmap.auth

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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
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
import org.btcmap.util.ApiRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInErrorTest {

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun signIn_whenFailureHasNoMessage_showsSignInErrorMessage() {
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

                onView(withId(R.id.signInOption)).inRoot(isDialog()).perform(click())
                onView(withId(R.id.usernameInput)).inRoot(isDialog())
                    .perform(typeText("satoshi"), closeSoftKeyboard())
                onView(withId(R.id.passwordInput)).inRoot(isDialog())
                    .perform(typeText("hunter2"), closeSoftKeyboard())

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
