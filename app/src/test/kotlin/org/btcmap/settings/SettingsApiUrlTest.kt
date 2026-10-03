package org.btcmap.settings

import kotlinx.coroutines.runBlocking
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Test

class SettingsApiUrlTest {

    private fun createDatabase() = runBlocking { Database(BundledSQLiteDriver(), ":memory:").apply { connect() } }

    private fun createSettings(db: Database) = Settings(
        dbProvider = { db },
        legacyValues = { emptyMap() },
    )

    @Test
    fun defaultsToPublicApi() = runBlocking<Unit> {
        val settings = createSettings(createDatabase())

        Assert.assertEquals("https://api.btcmap.org", settings.apiUrl.toString())
    }

    @Test
    fun usesStoredUrl() = runBlocking<Unit> {
        val settings = createSettings(createDatabase())

        settings.apiUrl = "https://staging.example.com".toUrl()

        Assert.assertEquals("https://staging.example.com", settings.apiUrl.toString())
    }

    @Test
    fun malformedStoredUrlFallsBackToPublicApi() = runBlocking<Unit> {
        val db = createDatabase()
        db.preference.upsert("apiUrl", "not a url")

        Assert.assertEquals(
            "https://api.btcmap.org",
            createSettings(db).apiUrl.toString(),
        )
    }
}
