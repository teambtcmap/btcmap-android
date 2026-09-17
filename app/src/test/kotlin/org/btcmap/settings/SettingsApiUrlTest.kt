package org.btcmap.settings

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test

class SettingsApiUrlTest {

    private fun createDatabase() = Database(BundledSQLiteDriver(), ":memory:")

    private fun createSettings(db: Database) = Settings(
        dbProvider = { db },
        legacyValues = { emptyMap() },
    )

    @Test
    fun defaultsToPublicApi() {
        val settings = createSettings(createDatabase())

        Assert.assertEquals("https://api.btcmap.org/", settings.apiUrl.toString())
    }

    @Test
    fun usesStoredUrl() {
        val settings = createSettings(createDatabase())

        settings.apiUrl = "https://staging.example.com".toHttpUrl()

        Assert.assertEquals("https://staging.example.com/", settings.apiUrl.toString())
    }

    @Test
    fun malformedStoredUrlFallsBackToPublicApi() {
        val db = createDatabase()
        db.preference.upsert("apiUrl", "not a url")

        Assert.assertEquals(
            "https://api.btcmap.org/",
            createSettings(db).apiUrl.toString(),
        )
    }
}
