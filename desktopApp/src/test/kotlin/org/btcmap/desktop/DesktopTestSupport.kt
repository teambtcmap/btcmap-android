package org.btcmap.desktop

import kotlinx.coroutines.runBlocking
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.util.toUrl
import java.nio.file.Files

/**
 * Test doubles for the desktop screens: an isolated database, its settings, and
 * an [Api] that is never called. Rendering a screen only reads the settings
 * (which loads them from the database once), so the screens can be clicked
 * through without a server or a window.
 */

internal fun testDatabase(): Database = runBlocking {
    Database(BundledSQLiteDriver(), Files.createTempFile("desktop-test", ".db").toString()).apply { connect() }
}

internal fun testSettings(db: Database): Settings = runBlocking {
    Settings(dbProvider = { db }, legacyValues = { emptyMap() }).apply { preload() }
}

internal fun testApi(): Api = Api(
    httpClient = apiHttpClient("btcmap-desktop-test"),
    baseUrl = { "https://api.example".toUrl() },
    token = { null },
    userAgent = "btcmap-desktop-test",
)
