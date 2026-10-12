package org.btcmap.settings

import kotlinx.coroutines.runBlocking
import org.btcmap.db.testDatabase
import org.junit.Assert
import org.junit.Test

/**
 * The stored session token: it is written through to the database, read back
 * through the in-memory cache, and cleared on sign-out. Ported from the Android
 * instrumented suite, which was the only place exercising [Settings] directly.
 */
class AuthTokenPersistenceTest {

    @Test
    fun storesTokenInTheDatabase() = runBlocking<Unit> {
        val db = testDatabase()
        val prefs = Settings(dbProvider = { db }, legacyValues = { emptyMap() })

        prefs.setAuthTokenForTesting("secret-token")

        Assert.assertEquals("secret-token", prefs.authToken)
        Assert.assertEquals("secret-token", db.preference.select(KEY_AUTH_TOKEN))
        Assert.assertTrue(prefs.authorized)
    }

    @Test
    fun clearingTokenSignsOut() = runBlocking<Unit> {
        val db = testDatabase()
        val prefs = Settings(dbProvider = { db }, legacyValues = { emptyMap() })

        prefs.setAuthTokenForTesting("secret-token")

        prefs.clearSession(db)

        Assert.assertNull(prefs.authToken)
        Assert.assertFalse(prefs.authorized)
        Assert.assertNull(db.preference.select(KEY_AUTH_TOKEN))
    }
}
