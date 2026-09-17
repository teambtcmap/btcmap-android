package org.btcmap.auth

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.btcmap.api.CreateTokenResponse
import org.btcmap.api.User
import org.btcmap.db.Database
import org.btcmap.db.table.user.SavedItem
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
        val db = Database(BundledSQLiteDriver(), ":memory:")
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
        Assert.assertEquals(listOf(SavedItem(id = 10, name = "Bitcoin Cafe")), stored.savedPlaces)
        Assert.assertEquals(listOf(SavedItem(id = 20, name = "Downtown")), stored.savedAreas)
    }

    @Test
    fun replacesThePreviousAccount() = runTest {
        val db = Database(BundledSQLiteDriver(), ":memory:")
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
}
