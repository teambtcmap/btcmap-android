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
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.db.table.user.User as DbUser
import org.btcmap.db.table.user.UserStore
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
    fun replacesCachedUserWhenSigningInAsDifferentAccount() = runBlocking {
        // A stale row from an earlier session must not survive a sign-in as a
        // different account: the table is keyed by user id and select() has no
        // ordering, so a leftover row could otherwise be shown instead.
        databaseRule.db.user.insert(dbUser(id = 99, name = "stale"))

        storeSignedInSession(databaseRule.db, prefs, response("token-1"))

        val user = databaseRule.db.user.select()
        Assert.assertNotNull(user)
        Assert.assertEquals(1L, user!!.id)
        Assert.assertEquals("satoshi", user.name)
    }

    @Test
    fun rollsBackTokenWhenCachedUserCannotBeStored() = runBlocking {
        val db = Database(FailingUserInsertDriver(), ":memory:")

        val error = try {
            storeSignedInSession(db, prefs, response("token-1"))
            null
        } catch (t: Throwable) {
            t
        }

        Assert.assertNotNull(error)
        Assert.assertNull(prefs.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun restoresPreviousSessionWhenCachedUserCannotBeStored() = runBlocking {
        // Both the token and the cached user of the previous session must come
        // back when storing the new cached user fails, instead of the user being
        // silently signed out.
        prefs.setAuthTokenForTesting("old-token")
        val db = Database(FailingUserInsertDriver(failOnUserInsert = 2), ":memory:")
        db.user.insert(dbUser(id = 99, name = "stale"))

        val error = try {
            storeSignedInSession(db, prefs, response("new-token"))
            null
        } catch (t: Throwable) {
            t
        }

        Assert.assertNotNull(error)
        Assert.assertEquals("old-token", prefs.authToken)
        Assert.assertEquals(99L, db.user.select()!!.id)
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

    /**
     * Fails the [failOnUserInsert]-th cached-user write (1-based), then delegates
     * normally, so a rollback that restores the previous user can succeed. The
     * user is stored as a preference under [UserStore.KEY], so the failing write
     * is identified by the bound key rather than the SQL text alone.
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
                    val statement = connection.prepare(sql)
                    if (!sql.contains("INSERT OR REPLACE INTO pref", ignoreCase = true)) {
                        return statement
                    }
                    return object : SQLiteStatement by statement {
                        private var isUser = false
                        private var counted = false

                        override fun bindText(index: Int, value: String) {
                            if (index == 1 && value == UserStore.KEY) isUser = true
                            statement.bindText(index, value)
                        }

                        override fun step(): Boolean {
                            if (isUser && !counted) {
                                counted = true
                                userInsertIndex++
                                if (userInsertIndex == failOnUserInsert) {
                                    throw RuntimeException("user insert failed")
                                }
                            }
                            return statement.step()
                        }
                    }
                }

                override fun close() {
                    connection.close()
                }
            }
        }
    }
}
