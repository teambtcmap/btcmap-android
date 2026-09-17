package org.btcmap.auth

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.btcmap.api.CreateTokenResponse
import org.btcmap.api.User
import org.btcmap.db.Database
import org.btcmap.settings.authToken
import org.btcmap.settings.getStoredAuthToken
import org.btcmap.settings.restoreStoredAuthTokenIf
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.db.table.user.User as DbUser
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignedInSessionStoreTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val prefs get() = preferencesRule.prefs

    @Test
    fun storesTokenAndCachedUser() = runBlocking {
        storeSignedInSession(databaseRule.db, prefs, response("token-1"))

        Assert.assertEquals("token-1", prefs.authToken)
        val user = databaseRule.db.user.select()
        Assert.assertNotNull(user)
        Assert.assertEquals(1L, user!!.id)
        Assert.assertEquals("satoshi", user.name)
    }

    @Test
    fun keepsExistingSessionWhenTokenCannotBeStored() = runBlocking {
        // A previous session is already stored and the new token cannot be
        // encrypted. Nothing was written for the new sign-in, so the previous
        // session must survive instead of being signed out.
        prefs.authToken = "old-token"
        databaseRule.db.user.insert(dbUser(id = 99, name = "stale"))

        val error = withUnavailableKeystore {
            runCatching { storeSignedInSession(databaseRule.db, prefs, response("new-token")) }
                .exceptionOrNull()
        }

        Assert.assertNotNull(error)
        Assert.assertEquals("old-token", prefs.authToken)
        Assert.assertEquals(99L, databaseRule.db.user.select()!!.id)
    }

    @Test
    fun rollsBackTokenWhenCachedUserCannotBeStored() = runBlocking {
        val db = Database(FailingUserInsertDriver(), ":memory:")

        val error = runCatching {
            storeSignedInSession(db, prefs, response("token-1"))
        }.exceptionOrNull()

        Assert.assertNotNull(error)
        Assert.assertNull(prefs.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun restoresPreviousSessionWhenCachedUserCannotBeStored() = runBlocking {
        // Both the token and the cached user of the previous session must come
        // back when storing the new cached user fails, instead of the user being
        // silently signed out.
        prefs.authToken = "old-token"
        val db = Database(FailingUserInsertDriver(failOnUserInsert = 2), ":memory:")
        db.user.insert(dbUser(id = 99, name = "stale"))

        val error = runCatching {
            storeSignedInSession(db, prefs, response("new-token"))
        }.exceptionOrNull()

        Assert.assertNotNull(error)
        Assert.assertEquals("old-token", prefs.authToken)
        Assert.assertEquals(99L, db.user.select()!!.id)
    }

    @Test
    fun rollbackRestoresPreviousToken() = runBlocking {
        prefs.authToken = "old-token"
        val previous = prefs.getStoredAuthToken()
        prefs.authToken = "new-token"
        val current = prefs.getStoredAuthToken()

        Assert.assertTrue(prefs.restoreStoredAuthTokenIf(current = current, previous = previous))
        Assert.assertEquals("old-token", prefs.authToken)
        Assert.assertEquals(previous, prefs.getStoredAuthToken())
    }

    @Test
    fun rollbackDoesNotReplaceNewerToken() = runBlocking {
        // Simulate a concurrent sign-in that replaced the token between the write
        // and the rollback of a failed one: the newer token must be kept.
        prefs.authToken = "old-token"
        val previous = prefs.getStoredAuthToken()
        prefs.authToken = "failed-token"
        val failed = prefs.getStoredAuthToken()
        prefs.authToken = "newer-token"

        Assert.assertFalse(prefs.restoreStoredAuthTokenIf(current = failed, previous = previous))
        Assert.assertEquals("newer-token", prefs.authToken)
    }

    @Test
    fun rollbackWorksWhenKeystoreIsUnavailable() = runBlocking {
        prefs.authToken = "old-token"
        val previous = prefs.getStoredAuthToken()
        prefs.authToken = "new-token"
        val current = prefs.getStoredAuthToken()

        withUnavailableKeystore {
            // Raw compare-and-set must not depend on being able to decrypt, so a
            // temporarily unavailable keystore cannot strand a new token.
            Assert.assertTrue(prefs.restoreStoredAuthTokenIf(current = current, previous = previous))
        }

        Assert.assertEquals("old-token", prefs.authToken)
    }

    private fun response(token: String) = CreateTokenResponse(
        token = token,
        user = User(
            id = 1,
            name = "satoshi",
            roles = listOf("user"),
            savedPlaces = emptyList(),
            savedAreas = emptyList(),
        ),
    )

    private fun dbUser(id: Long, name: String) = DbUser(
        id = id,
        name = name,
        roles = emptyList(),
        savedPlaces = emptyList(),
        savedAreas = emptyList(),
    )

    private inline fun <T> withUnavailableKeystore(block: () -> T): T =
        TokenCipher.withKeyProvider({ throw IllegalStateException("keystore unavailable") }, block)

    /**
     * Fails the [failOnUserInsert]-th cached-user insert (1-based), then delegates
     * normally, so a rollback that re-inserts the previous user can succeed.
     */
    private class FailingUserInsertDriver(
        private val failOnUserInsert: Int = 1,
    ) : SQLiteDriver {
        private var userInsertIndex = 0
        private val delegate = AndroidSQLiteDriver()

        override fun open(fileName: String): SQLiteConnection {
            val connection = delegate.open(fileName)
            return object : SQLiteConnection {
                override fun prepare(sql: String): SQLiteStatement {
                    val isUserInsert =
                        sql.contains("INSERT INTO user", ignoreCase = true) ||
                            sql.contains("INSERT OR REPLACE INTO user", ignoreCase = true)
                    if (isUserInsert) {
                        userInsertIndex++
                        if (userInsertIndex == failOnUserInsert) {
                            throw RuntimeException("user insert failed")
                        }
                    }
                    return connection.prepare(sql)
                }

                override fun close() {
                    connection.close()
                }
            }
        }
    }
}
