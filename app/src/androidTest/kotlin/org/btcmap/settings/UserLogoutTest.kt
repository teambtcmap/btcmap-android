package org.btcmap.settings

import android.view.View
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.user.User
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserLogoutTest {

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

    private val prefs get() = preferencesRule.prefs

    @Test
    fun logout_clearsLocalSessionAndRevokesTokenServerSide() {
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
                lateinit var profile: UserProfileFragment
                scenario.onActivity { activity ->
                    profile = UserProfileFragment()
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, profile, PROFILE_TAG)
                    }
                }

                waitUntil {
                    var shown = false
                    scenario.onActivity {
                        shown = profile.requireView().findViewById<View>(R.id.logoutButton) != null
                    }
                    shown
                }

                scenario.onActivity {
                    profile.requireView().findViewById<View>(R.id.logoutButton).performClick()
                }

                waitUntil { prefs.authToken == null && databaseRule.db.user.select() == null }

                Assert.assertNull(databaseRule.db.user.select())
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
