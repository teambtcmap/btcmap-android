package org.btcmap

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.db.Database
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authToken
import org.btcmap.settings.prefs
import org.maplibre.android.MapLibre
import org.btcmap.settings.init as settingsInit
import org.btcmap.util.init as typefaceInit

class App : Application() {
    internal var apiForTesting: Api? = null

    internal var dbForTesting: Database? = null

    internal var mapStyleUriForTesting: String? = null

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sync: Sync
        get() = Sync(api, db)

    val api: Api
        get() = apiForTesting ?: defaultApi

    private val defaultApi: Api by lazy {
        Api(
            httpClient = apiHttpClient(),
            baseUrl = { prefs.apiUrl },
            onUnauthorized = {
                withContext(Dispatchers.IO) {
                    runCatching {
                        prefs.authToken = null
                        db.user.delete()
                    }
                }
            },
        )
    }

    val db: Database
        get() = dbForTesting ?: defaultDb

    private val defaultDb: Database by lazy {
        Database(
            driver = AndroidSQLiteDriver(),
            path = getDatabasePath("btcmap-2025-11-06.db").absolutePath,
        )
    }

    override fun onCreate() {
        super.onCreate()
        settingsInit(this)
        typefaceInit(this)
        MapLibre.getInstance(this)

        // Decrypt the stored session token up front so later reads from the main
        // thread hit the in-memory cache instead of the keystore.
        ioScope.launch { runCatching { prefs.authToken } }
    }
}

fun Fragment.sync(): Sync = (requireContext().applicationContext as App).sync

fun Fragment.api(): Api = (requireContext().applicationContext as App).api

fun Fragment.db(): Database = (requireContext().applicationContext as App).db