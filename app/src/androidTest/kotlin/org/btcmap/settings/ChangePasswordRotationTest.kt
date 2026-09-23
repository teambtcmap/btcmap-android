package org.btcmap.settings

import android.view.View
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
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
import org.btcmap.db.table.user.User
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Covers the change-password form across a configuration change.
 *
 * The password fields opt out of view-state saving, so the form is kept in a
 * retained view model; and the request runs in a retained view model rather
 * than the view's scope, so a rotation mid-request no longer cancels it or
 * loses its outcome. This drives the real [UserProfileFragment] so both the
 * keyboard entry and the failure dialog are exercised on a device.
 */
@RunWith(AndroidJUnit4::class)
class ChangePasswordRotationTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun credentialsSubmittedAfterRotation_reachTheServer() {
        insertSignedInUser()

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                val profile = showProfile(scenario)
                openChangePasswordDialog(scenario, profile)
                fillPasswordFields()

                scenario.recreate()

                // The password fields opt out of view-state saving, so their
                // restored text proves the retained change-password form.
                onView(withId(R.id.currentPasswordInput)).inRoot(isDialog())
                    .check(matches(withText(CURRENT_PASSWORD)))
                onView(withId(R.id.newPasswordInput)).inRoot(isDialog())
                    .check(matches(withText(NEW_PASSWORD)))
                onView(withId(R.id.confirmPasswordInput)).inRoot(isDialog())
                    .check(matches(withText(NEW_PASSWORD)))

                onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())

                val request = waitForPasswordRequest()
                Assert.assertEquals("PUT", request.method)
                Assert.assertEquals("/v4/users/me/password", request.url.encodedPath)
                Assert.assertEquals(
                    """{"old_password":"$CURRENT_PASSWORD","new_password":"$NEW_PASSWORD"}""",
                    request.body?.utf8(),
                )
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    @Test
    fun failureIsDeliveredWhenRotationHappensInFlight() {
        insertSignedInUser()

        // Delay the rejection so the screen can be recreated while the request
        // is still pending; the outcome must still reach the recreated screen.
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse.Builder()
                    .code(400)
                    .addHeader("Content-Type", "application/json")
                    .body("""{"message":"$ERROR_MESSAGE"}""")
                    .bodyDelay(2, TimeUnit.SECONDS)
                    .build()
        }

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                val profile = showProfile(scenario)
                openChangePasswordDialog(scenario, profile)
                fillPasswordFields()

                onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
                waitUntil { apiRule.server.requestCount > 0 }

                scenario.recreate()

                waitUntil {
                    try {
                        onView(withText(ERROR_MESSAGE)).inRoot(isDialog())
                            .check(matches(isDisplayed()))
                        true
                    } catch (t: Throwable) {
                        false
                    }
                }

                onView(withText(ERROR_MESSAGE)).inRoot(isDialog())
                    .check(matches(isDisplayed()))
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun showProfile(scenario: ActivityScenario<Activity>): UserProfileFragment {
        lateinit var profile: UserProfileFragment
        scenario.onActivity { activity ->
            profile = UserProfileFragment()
            activity.supportFragmentManager.commitNow {
                setReorderingAllowed(true)
                replace(R.id.fragmentContainerView, profile, PROFILE_TAG)
            }
        }

        waitUntil {
            var ready = false
            scenario.onActivity {
                ready = profile.requireView()
                    .findViewById<View>(R.id.changePasswordButton) != null
            }
            ready
        }
        return profile
    }

    private fun openChangePasswordDialog(
        scenario: ActivityScenario<Activity>,
        profile: UserProfileFragment,
    ) {
        scenario.onActivity {
            profile.requireView()
                .findViewById<View>(R.id.changePasswordButton)
                .performClick()
        }
    }

    private fun fillPasswordFields() {
        onView(withId(R.id.currentPasswordInput)).inRoot(isDialog())
            .perform(typeText(CURRENT_PASSWORD), closeSoftKeyboard())
        onView(withId(R.id.newPasswordInput)).inRoot(isDialog())
            .perform(typeText(NEW_PASSWORD), closeSoftKeyboard())
        onView(withId(R.id.confirmPasswordInput)).inRoot(isDialog())
            .perform(typeText(NEW_PASSWORD), closeSoftKeyboard())
    }

    private fun waitForPasswordRequest(): RecordedRequest {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            if (apiRule.server.requestCount > 0) {
                val request = apiRule.server.takeRequest()
                if (request.url.encodedPath == "/v4/users/me/password") return request
            } else {
                Thread.sleep(50)
            }
        }
        throw AssertionError("Expected a change-password request to /v4/users/me/password")
    }

    private fun insertSignedInUser() {
        databaseRule.db.user.insert(
            User(
                id = 1,
                name = "satoshi",
                roles = emptyList(),
                savedPlaces = emptyList(),
                savedAreas = emptyList(),
            )
        )
    }

    companion object {
        private const val PROFILE_TAG = "profile"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
        private const val CURRENT_PASSWORD = "hunter2"
        private const val NEW_PASSWORD = "newpassword"
        private const val ERROR_MESSAGE = "Invalid old password"
    }
}
