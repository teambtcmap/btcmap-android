package org.btcmap.settings

import org.btcmap.i18n.Strings

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import org.btcmap.nav.AppRootFragment
import org.btcmap.ui.AppRoute
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.user.User
import org.btcmap.ui.PROFILE_PASSWORD_CONFIRM_TAG
import org.btcmap.ui.PROFILE_PASSWORD_CURRENT_TAG
import org.btcmap.ui.PROFILE_PASSWORD_NEW_TAG
import org.btcmap.ui.PROFILE_PASSWORD_SAVE_TAG
import org.btcmap.util.AppTestCase
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The profile screen's inline change-password form. It is now the shared Compose
 * form, so a rotation simply recreates it rather than retaining an in-flight
 * request; this drives the form through to the server.
 */
@RunWith(AndroidJUnit4::class)
class ChangePasswordTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun changePassword_reachesTheServer() = runBlocking<Unit> {
        insertSignedInUser()

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                showProfile(scenario)
                openChangePasswordForm()
                fillPasswordFields()

                composeTestRule.onNodeWithTag(PROFILE_PASSWORD_SAVE_TAG).performClick()

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

    private fun showProfile(scenario: ActivityScenario<Activity>) {
        scenario.onActivity { activity ->
            activity.supportFragmentManager.commitNow {
                setReorderingAllowed(true)
                replace(R.id.fragmentContainerView, AppRootFragment.create(AppRoute.UserProfile), PROFILE_TAG)
            }
        }

        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodesWithContentDescription(Strings.current()["change_password"])
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun openChangePasswordForm() {
        composeTestRule
            .onNodeWithContentDescription(Strings.current()["change_password"])
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag(PROFILE_PASSWORD_CURRENT_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun fillPasswordFields() {
        composeTestRule
            .onNodeWithTag(PROFILE_PASSWORD_CURRENT_TAG)
            .performTextInput(CURRENT_PASSWORD)
        composeTestRule
            .onNodeWithTag(PROFILE_PASSWORD_NEW_TAG)
            .performTextInput(NEW_PASSWORD)
        composeTestRule
            .onNodeWithTag(PROFILE_PASSWORD_CONFIRM_TAG)
            .performTextInput(NEW_PASSWORD)
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
        runBlocking {
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
    }

    companion object {
        private const val PROFILE_TAG = "profile"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
        private const val CURRENT_PASSWORD = "hunter2"
        private const val NEW_PASSWORD = "newpassword"
    }
}
