package org.btcmap.account

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.settings.authToken
import org.junit.Assert
import org.junit.Test

class AccountSessionTest {

    private fun database() = Database(BundledSQLiteDriver(), ":memory:")

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
}
