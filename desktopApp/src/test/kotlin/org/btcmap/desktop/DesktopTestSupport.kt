package org.btcmap.desktop

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import java.nio.file.Files

/**
 * Test doubles for the desktop screens: an isolated database, its settings, and
 * an [Api] that is never called. Rendering a screen only reads the settings
 * (which loads them from the database once), so the screens can be clicked
 * through without a server or a window.
 */

internal fun testDatabase(): Database =
    Database(BundledSQLiteDriver(), Files.createTempFile("desktop-test", ".db").toString())

internal fun testSettings(db: Database): Settings =
    Settings(dbProvider = { db }, legacyValues = { emptyMap() }).apply { preload() }

internal fun testApi(): Api = Api(
    httpClient = apiHttpClient(
        userAgent = "btcmap-desktop-test",
        token = { null },
        apiUrl = { "https://api.example".toHttpUrl() },
    ),
    baseUrl = { "https://api.example".toHttpUrl() },
    userAgent = "btcmap-desktop-test",
)
