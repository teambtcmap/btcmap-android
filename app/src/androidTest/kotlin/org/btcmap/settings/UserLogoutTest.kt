package org.btcmap.settings

import org.btcmap.i18n.Strings

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserLogoutTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    private val prefs get() = preferencesRule.prefs

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun logout_clearsLocalSessionAndRevokesTokenServerSide() = runBlocking<Unit> {
        prefs.setAuthTokenForTesting("token-1")
        databaseRule.db.user.insert(
            User(
                id = 1,
                name = "satoshi",
                roles = emptyList(),
                savedPlaces = emptyList(),
                savedAreas = emptyList(),
            )
        )

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, AppRootFragment.create(AppRoute.UserProfile), PROFILE_TAG)
                    }
                }

                val logOut = Strings.current()["logout"]
                composeTestRule.waitUntil(5_000) {
                    composeTestRule.onAllNodesWithText(logOut).fetchSemanticsNodes().isNotEmpty()
                }
                composeTestRule.onNodeWithText(logOut).performScrollTo().performClick()

                waitUntil { prefs.authToken == null && runBlocking { databaseRule.db.user.select() } == null }

                Assert.assertNull(runBlocking { databaseRule.db.user.select() })
                Assert.assertNull(prefs.authToken)

                val request = waitForSignOutRequest()
                Assert.assertEquals("POST", request.method)
                Assert.assertEquals("/v4/auth/signout", request.url.encodedPath)
                Assert.assertEquals("Bearer token-1", request.headers["Authorization"])
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun waitForSignOutRequest(): RecordedRequest {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            if (apiRule.server.requestCount > 0) {
                val request = apiRule.server.takeRequest()
                if (request.url.encodedPath == "/v4/auth/signout") return request
            } else {
                Thread.sleep(50)
            }
        }
        throw AssertionError("Expected a sign-out request to /v4/auth/signout")
    }

    companion object {
        private const val PROFILE_TAG = "profile"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
