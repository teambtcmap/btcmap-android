package org.btcmap.account

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.btcmap.db.table.user.User
import org.btcmap.settings.Settings
import org.btcmap.settings.authToken
import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class AccountSessionTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun database() = org.btcmap.db.testDatabase()

    private fun settings(db: Database) = Settings(dbProvider = { db }, legacyValues = { emptyMap() })

    @Test
    fun clearSession_returnsTheTokenItCleared() = runTest {
        val db = database()
        val settings = settings(db)
        settings.setAuthTokenForTesting("token-1")

        val cleared = AccountSession.clearSession(db, settings)

        Assert.assertEquals("token-1", cleared)
        Assert.assertNull(settings.authToken)
    }

    @Test
    fun clearSession_isNullWhenThereIsNoSession() = runTest {
        val db = database()
        val settings = settings(db)

        Assert.assertNull(AccountSession.clearSession(db, settings))
    }

    private fun createApi() = Api(
        httpClient = HttpClient(CIO),
        baseUrl = { serverRule.server.url("/").toString().toUrl() },
    )

    /** Enqueues a `GET /v4/users/me` response with the given role list. */
    private fun enqueueUser(roles: List<String>) {
        val rolesJson = roles.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        serverRule.server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(
                    """
                    {
                        "id": 1,
                        "name": "satoshi",
                        "roles": $rolesJson,
                        "saved_places": [],
                        "saved_areas": []
                    }
                    """.trimIndent(),
                )
                .build(),
        )
    }

    private suspend fun signedIn(db: Database, settings: Settings, roles: List<String>) {
        settings.setAuthTokenForTesting("token-1")
        db.user.insert(
            User(id = 1, name = "satoshi", roles = roles, savedPlaces = emptyList(), savedAreas = emptyList()),
        )
    }

    @Test
    fun refresh_replacesTheCachedProfileAndReportsTheChange() = runTest {
        val db = database()
        val settings = settings(db)
        signedIn(db, settings, roles = listOf("user"))

        enqueueUser(roles = listOf("user", "event_manager"))

        val changed = AccountSession.refresh(createApi(), db, settings)

        Assert.assertTrue(changed)
        Assert.assertEquals(listOf("user", "event_manager"), db.user.select()!!.roles)
    }

    @Test
    fun refresh_returnsFalseWhenNothingChanged() = runTest {
        val db = database()
        val settings = settings(db)
        signedIn(db, settings, roles = listOf("user"))

        enqueueUser(roles = listOf("user"))

        Assert.assertFalse(AccountSession.refresh(createApi(), db, settings))
    }

    @Test
    fun refresh_isSkippedWhenSignedOut() = runTest {
        val db = database()
        val settings = settings(db)

        // No token: the refresh must not hit the network.
        Assert.assertFalse(AccountSession.refresh(createApi(), db, settings))
        Assert.assertEquals(0, serverRule.server.requestCount)
    }
}
