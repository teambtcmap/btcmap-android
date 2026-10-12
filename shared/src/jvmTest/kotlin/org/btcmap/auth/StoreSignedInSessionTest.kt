package org.btcmap.auth

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.btcmap.api.CreateTokenResponse
import org.btcmap.api.SavedItem
import org.btcmap.api.User
import org.btcmap.db.Database
import org.btcmap.db.table.user.SavedItem as DbSavedItem
import org.btcmap.db.table.user.User as DbUser
import org.btcmap.db.table.user.UserStore
import org.btcmap.settings.Settings
import org.btcmap.settings.authToken
import org.junit.Assert
import org.junit.Test

class StoreSignedInSessionTest {

    private fun createSettings(db: Database) = Settings(
        dbProvider = { db },
        legacyValues = { emptyMap() },
    )

    @Test
    fun storesTokenUserAndSavedItems() = runTest {
        val db = org.btcmap.db.testDatabase()
        val settings = createSettings(db)

        storeSignedInSession(
            db = db,
            prefs = settings,
            response = CreateTokenResponse(
                token = "token-1",
                user = User(
                    id = 1,
                    name = "satoshi",
                    roles = listOf("user"),
                    savedPlaces = listOf(SavedItem(id = 10, name = "Bitcoin Cafe")),
                    savedAreas = listOf(SavedItem(id = 20, name = "Downtown")),
                ),
            ),
        )

        Assert.assertEquals("token-1", settings.authToken)

        val stored = db.user.select()
        Assert.assertNotNull(stored)
        Assert.assertEquals(1L, stored!!.id)
        Assert.assertEquals("satoshi", stored.name)
        Assert.assertEquals(listOf("user"), stored.roles)
        Assert.assertEquals(listOf(DbSavedItem(id = 10, name = "Bitcoin Cafe")), stored.savedPlaces)
        Assert.assertEquals(listOf(DbSavedItem(id = 20, name = "Downtown")), stored.savedAreas)
    }

    @Test
    fun replacesThePreviousAccount() = runTest {
        val db = org.btcmap.db.testDatabase()
        val settings = createSettings(db)

        storeSignedInSession(db, settings, response(token = "old-token", userId = 1, name = "satoshi"))
        storeSignedInSession(db, settings, response(token = "new-token", userId = 2, name = "alice"))

        Assert.assertEquals("new-token", settings.authToken)
        val stored = db.user.select()
        Assert.assertNotNull(stored)
        Assert.assertEquals(2L, stored!!.id)
        Assert.assertEquals("alice", stored.name)
    }

    private fun response(token: String, userId: Long, name: String) = CreateTokenResponse(
        token = token,
        user = User(
            id = userId,
            name = name,
            roles = emptyList(),
            savedPlaces = emptyList(),
            savedAreas = emptyList(),
        ),
    )

    @Test
    fun rollsBackTokenWhenCachedUserCannotBeStored() = runTest {
        val db = Database(FailingUserInsertDriver(), ":memory:")
        db.connect()
        val settings = createSettings(db)

        val error = runCatching {
            storeSignedInSession(db, settings, response(token = "token-1", userId = 1, name = "satoshi"))
        }.exceptionOrNull()

        Assert.assertNotNull(error)
        Assert.assertNull(settings.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun restoresPreviousSessionWhenCachedUserCannotBeStored() = runTest {
        // Both the token and the cached user of the previous session must come
        // back when storing the new cached user fails, instead of the user being
        // silently signed out.
        val db = Database(FailingUserInsertDriver(failOnUserInsert = 2), ":memory:")
        db.connect()
        val settings = createSettings(db)
        settings.setAuthTokenForTesting("old-token")
        db.user.insert(dbUser(id = 99, name = "stale"))

        val error = runCatching {
            storeSignedInSession(db, settings, response(token = "new-token", userId = 1, name = "satoshi"))
        }.exceptionOrNull()

        Assert.assertNotNull(error)
        Assert.assertEquals("old-token", settings.authToken)
        Assert.assertEquals(99L, db.user.select()!!.id)
    }

    private fun dbUser(id: Long, name: String) = DbUser(
        id = id,
        name = name,
        roles = emptyList(),
        savedPlaces = emptyList(),
        savedAreas = emptyList(),
    )
}

/**
 * Fails the [failOnUserInsert]-th cached-user write (1-based), then delegates
 * normally, so a rollback that restores the previous user can succeed. The user
 * is stored as a preference under [UserStore.KEY], so the failing write is
 * identified by the bound key rather than the SQL text alone.
 */
private class FailingUserInsertDriver(
    private val failOnUserInsert: Int = 1,
) : SQLiteDriver {
    private var userInsertIndex = 0
    private val delegate = BundledSQLiteDriver()

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
