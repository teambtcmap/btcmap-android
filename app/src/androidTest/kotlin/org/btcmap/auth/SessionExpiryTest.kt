package org.btcmap.auth

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.btcmap.App
import org.btcmap.api.ApiException
import org.btcmap.api.getUser
import org.btcmap.db.table.user.User
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authToken
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the full session-expiry flow through the real app API, which owns
 * the `onUnauthorized` handler that clears the stored token and cached user.
 */
@RunWith(AndroidJUnit4::class)
class SessionExpiryTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    private val prefs get() = preferencesRule.prefs

    private val server = MockWebServer()

    @Before
    fun startServer() {
        server.start()
        // This test exercises the real app API, whose onUnauthorized handler
        // clears the session. Drop the mock API installed by AppTestCase so
        // app.api falls back to the default API, which reads prefs.apiUrl.
        app.apiForTesting = null
    }

    @After
    fun stopServer() {
        server.close()
    }

    @Test
    fun rejectedToken_clearsStoredTokenAndCachedUser() {
        prefs.apiUrl = server.url("/")
        prefs.setAuthTokenForTesting("stale-token")
        databaseRule.db.user.insert(signedInUser())

        server.enqueue(
            MockResponse.Builder()
                .code(401)
                .addHeader("Content-Type", "application/json")
                .body("""{"message":"Authentication required"}""")
                .build()
        )

        val error = try {
            runBlocking { app.api.getUser() }
            null
        } catch (t: Throwable) {
            t
        }

        Assert.assertTrue("Expected ApiException but got $error", error is ApiException)
        Assert.assertEquals(401, (error as ApiException).code)
        Assert.assertEquals("Bearer stale-token", server.takeRequest().headers["Authorization"])

        waitUntil {
            prefs.authToken == null && databaseRule.db.user.select() == null
        }

        Assert.assertNull(prefs.authToken)
        Assert.assertNull(databaseRule.db.user.select())
    }

    @Test
    fun staleRejectedToken_doesNotClearNewerSession() {
        prefs.setAuthTokenForTesting("new-token")
        databaseRule.db.user.insert(signedInUser())

        // A 401 from a request that raced a fresh sign-in must not sign out the
        // session that is currently stored.
        runBlocking { app.handleUnauthorized("old-token") }

        Assert.assertEquals("new-token", prefs.authToken)
        Assert.assertNotNull(databaseRule.db.user.select())
    }

    @Test
    fun matchingRejectedToken_clearsSession() {
        prefs.setAuthTokenForTesting("same-token")
        databaseRule.db.user.insert(signedInUser())

        runBlocking { app.handleUnauthorized("same-token") }

        Assert.assertNull(prefs.authToken)
        Assert.assertNull(databaseRule.db.user.select())
    }

    private fun signedInUser() = User(
        id = 1,
        name = "satoshi",
        roles = listOf("user"),
        savedPlaces = emptyList(),
        savedAreas = emptyList(),
    )
}
