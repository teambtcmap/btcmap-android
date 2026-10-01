package org.btcmap.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.ui.AUTH_FIELD_TAG_PREFIX
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reproduces the rotation regression: the credential form kept its typed text
 * across a configuration change but lost the submit handler, so submitting
 * after a rotation silently dropped the credentials. This drives a real sign-in
 * through a mock server and asserts the recreated host is reached with the
 * extras the flow carried.
 */
@RunWith(AndroidJUnit4::class)
class AuthRotationTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun credentialsSubmittedAfterRotation_reachTheRecreatedHost() {
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

        AuthHostFragment.receivedExtras = null
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val host = AuthHostFragment()
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, host, HOST_TAG)
                    }
                    host.showAuthDialog(Bundle().apply { putString(EXTRA_MARKER, MARKER) })
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

                scenario.recreate()

                // The passwords opt out of view-state saving, so the fact that
                // they are still here proves the retained ViewModel is used.
                composeTestRule.onNodeWithText("satoshi").assertIsDisplayed()
                composeTestRule.onNodeWithText("hunter2").assertIsDisplayed()

                onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())

                waitUntil { AuthHostFragment.receivedExtras != null }
                Assert.assertEquals(MARKER, AuthHostFragment.receivedExtras?.getString(EXTRA_MARKER))
            }
        } finally {
            app.mapStyleUriForTesting = null
            AuthHostFragment.receivedExtras = null
        }
    }

    class AuthHostFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View = FrameLayout(requireContext())

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            registerAuthResultListener { extras -> receivedExtras = extras }
        }

        companion object {
            @Volatile
            var receivedExtras: Bundle? = null
        }
    }

    companion object {
        private const val HOST_TAG = "auth-rotation-host"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
        private const val EXTRA_MARKER = "marker"
        private const val MARKER = "resumed"
    }
}
