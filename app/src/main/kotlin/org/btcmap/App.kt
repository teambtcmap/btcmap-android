package org.btcmap

import android.app.Application
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.api.signOut
import org.btcmap.db.Database
import org.btcmap.settings.apiUrl
import org.btcmap.settings.prefs
import org.btcmap.util.rethrowIfCancellation
import org.maplibre.android.MapLibre
import org.btcmap.settings.init as settingsInit
import org.btcmap.util.init as typefaceInit

private const val TAG = "App"

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
            onUnauthorized = { handleUnauthorized(it) },
        )
    }

    /**
     * Clears the stored session after the server rejected a token. The clear is
     * atomic with the token comparison inside [Settings.clearSessionIfTokenMatches],
     * so a late 401 from a request that raced a fresh sign-in cannot sign the
     * user out again.
     */
    internal suspend fun handleUnauthorized(requestToken: String?) {
        withContext(Dispatchers.IO) {
            if (requestToken != null) {
                try {
                    prefs.clearSessionIfTokenMatches(db, requestToken)
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    Log.e(TAG, "Failed to clear rejected session", t)
                }
            }
        }
    }

    /**
     * Best-effort server-side revocation of a signed-out token. Local sign-out
     * must not depend on it, so failures are ignored and callers do not wait:
     * the token is already cleared on the device by the time this runs.
     */
    internal fun revokeToken(token: String) {
        ioScope.launch {
            try {
                api.signOut(token)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                Log.w(TAG, "Failed to revoke token", t)
            }
        }
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

        // Load the settings from the database up front so later reads from the
        // main thread hit the in-memory cache. The session token is part of the
        // cache, so this also makes it available without touching the database.
        ioScope.launch {
            try {
                prefs.preload()
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                Log.e(TAG, "Failed to preload settings", t)
            }
        }
    }
}

fun Fragment.sync(): Sync = (requireContext().applicationContext as App).sync

fun Fragment.api(): Api = (requireContext().applicationContext as App).api

fun Fragment.db(): Database = (requireContext().applicationContext as App).db

fun Fragment.app(): App = requireContext().applicationContext as App