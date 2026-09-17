package org.btcmap.settings

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.junit.Assert
import org.junit.Test

class SettingsSessionTest {

    private fun createDatabase(): Database {
        return Database(BundledSQLiteDriver(), ":memory:")
    }

    private fun createSettings(
        db: Database,
        clearLegacyValues: (Set<String>) -> Unit = {},
        legacyValues: () -> Map<String, Any?> = { emptyMap() },
    ): Settings {
        return Settings(
            dbProvider = { db },
            legacyValues = legacyValues,
            clearLegacyValues = clearLegacyValues,
        )
    }

    private fun user(
        id: Long,
        name: String,
        savedPlaces: List<SavedItem> = emptyList(),
    ): User {
        return User(
            id = id,
            name = name,
            roles = emptyList(),
            savedPlaces = savedPlaces,
            savedAreas = emptyList(),
        )
    }

    @Test
    fun replaceSession_storesTokenAndUser() {
        val db = createDatabase()
        val settings = createSettings(db)

        settings.replaceSession(db, token = "token-1", user = user(1, "satoshi"))

        Assert.assertEquals("token-1", settings.authToken)
        Assert.assertEquals("satoshi", db.user.select()?.name)
        Assert.assertTrue(settings.authorized)
    }

    @Test
    fun replaceSession_replacesPreviousAccount() {
        val db = createDatabase()
        val settings = createSettings(db)

        settings.replaceSession(db, token = "token-1", user = user(1, "satoshi"))
        settings.replaceSession(db, token = "token-2", user = user(2, "alice"))

        Assert.assertEquals("token-2", settings.authToken)
        val stored = db.user.select()
        Assert.assertNotNull(stored)
        Assert.assertEquals(2L, stored!!.id)
        Assert.assertEquals("alice", stored.name)
    }

    @Test
    fun replaceSession_nullTokenClearsSession() {
        val db = createDatabase()
        val settings = createSettings(db)
        settings.replaceSession(db, token = "token-1", user = user(1, "satoshi"))

        settings.replaceSession(db, token = null, user = null)

        Assert.assertNull(settings.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun clearSession_clearsTokenAndUser() {
        val db = createDatabase()
        val settings = createSettings(db)
        settings.replaceSession(db, token = "token-1", user = user(1, "satoshi"))

        settings.clearSession(db)

        Assert.assertNull(settings.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun clearSessionIfTokenMatches_clearsMatchingToken() {
        val db = createDatabase()
        val settings = createSettings(db)
        settings.replaceSession(db, token = "token-1", user = user(1, "satoshi"))

        val cleared = settings.clearSessionIfTokenMatches(db, "token-1")

        Assert.assertTrue(cleared)
        Assert.assertNull(settings.authToken)
        Assert.assertNull(db.user.select())
    }

    @Test
    fun clearSessionIfTokenMatches_ignoresStaleToken() {
        val db = createDatabase()
        val settings = createSettings(db)
        settings.replaceSession(db, token = "old-token", user = user(1, "satoshi"))
        // A fresh sign-in commits a new token before the late 401 arrives.
        settings.replaceSession(db, token = "new-token", user = user(1, "satoshi"))

        val cleared = settings.clearSessionIfTokenMatches(db, "old-token")

        Assert.assertFalse(cleared)
        Assert.assertEquals("new-token", settings.authToken)
        Assert.assertNotNull(db.user.select())
    }

    @Test
    fun clearSessionIfTokenMatches_doesNothingWhenSignedOut() {
        val db = createDatabase()
        val settings = createSettings(db)

        Assert.assertFalse(settings.clearSessionIfTokenMatches(db, "token-1"))
        Assert.assertNull(settings.authToken)
    }

    @Test
    fun authToken_doesNotLoadFromDatabase() {
        // The main-thread session reads must never open the database, so a
        // provider that would fail proves the getter stays in memory.
        val settings = Settings(
            dbProvider = { throw AssertionError("database must not be read") },
            legacyValues = { emptyMap() },
        )

        Assert.assertNull(settings.authToken)
        Assert.assertFalse(settings.authorized)
    }

    @Test
    fun importLegacy_keepsPlaintextTokenAndUser() {
        val db = createDatabase()
        db.user.insert(user(1, "satoshi"))

        val settings = createSettings(db) {
            mapOf("auth_token" to "token-1", "mapStyle" to "dark")
        }
        settings.preload()

        Assert.assertEquals("token-1", settings.authToken)
        Assert.assertNotNull(db.user.select())
        Assert.assertEquals("dark", settings.getString("mapStyle", null))
    }

    @Test
    fun importLegacy_dropsUndecryptableTokenAndStaleUser() {
        val db = createDatabase()
        // The previous version cached the account while the token was stored in
        // SharedPreferences, so a leftover row exists.
        db.user.insert(user(1, "satoshi"))

        val settings = createSettings(db) {
            mapOf("auth_token" to "enc:v1:garbage", "mapStyle" to "dark")
        }
        settings.preload()

        Assert.assertNull(settings.authToken)
        Assert.assertNull(db.user.select())
        Assert.assertEquals("dark", settings.getString("mapStyle", null))
    }

    @Test
    fun importLegacy_clearsImportedLegacyValues() {
        val db = createDatabase()
        val cleared = mutableSetOf<String>()

        val settings = createSettings(
            db = db,
            legacyValues = { mapOf("auth_token" to "token-1", "mapStyle" to "dark") },
            clearLegacyValues = { cleared += it },
        )
        settings.preload()

        Assert.assertEquals(setOf("auth_token", "mapStyle"), cleared)
    }

    @Test
    fun importLegacy_clearsUnusableEncryptedToken() {
        val db = createDatabase()
        val cleared = mutableSetOf<String>()

        val settings = createSettings(
            db = db,
            legacyValues = { mapOf("auth_token" to "enc:v1:garbage") },
            clearLegacyValues = { cleared += it },
        )
        settings.preload()

        Assert.assertEquals(setOf("auth_token"), cleared)
    }
}
