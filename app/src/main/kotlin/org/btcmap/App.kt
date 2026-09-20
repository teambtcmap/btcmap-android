package org.btcmap

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.sqlite.driver.AndroidSQLiteDriver
import coil3.ImageLoader
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.api.signOut
import org.btcmap.bundle.BundledAreas
import org.btcmap.bundle.BundledComments
import org.btcmap.bundle.BundledEvents
import org.btcmap.bundle.BundledPlaces
import org.btcmap.db.Database
import org.btcmap.db.LegacyDatabases
import org.btcmap.imagestats.ImageStatsEventListener
import org.btcmap.offline.OfflineMaps
import org.btcmap.settings.apiUrl
import org.btcmap.settings.prefs
import org.btcmap.util.rethrowIfCancellation
import org.maplibre.android.MapLibre
import org.btcmap.settings.init as settingsInit
import org.btcmap.util.init as typefaceInit

private const val DATABASE_NAME = "btcmap.db"

class App : Application(), SingletonImageLoader.Factory {
    internal var apiForTesting: Api? = null

    internal var dbForTesting: Database? = null

    internal var mapStyleUriForTesting: String? = null

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sync: Sync
        get() = Sync(api, db)

    /**
     * The sync manager the screens observe. The app-scoped default seeds the
     * database from the bundled snapshots, which is the only Android-specific
     * part of the sync: [SyncManager] itself only sees the seed results.
     *
     * Read through a getter so the `*ForTesting` overrides above still apply
     * whenever the manager is built.
     */
    internal val syncController: SyncController
        get() = syncControllerForTesting ?: defaultSyncManager

    internal var syncControllerForTesting: SyncController? = null

    private val defaultSyncManager: SyncManager by lazy {
        SyncManager(
            sync = { sync },
            seedPlaces = { BundledPlaces.import(this, db).placesImported },
            seedEvents = { BundledEvents.import(this, db).eventsImported },
            seedComments = { BundledComments.import(this, db).commentsImported },
            seedAreas = { BundledAreas.import(this, db).areasImported },
        )
    }

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
            }
        }
    }

    val db: Database
        get() = dbForTesting ?: defaultDb

    /**
     * Owns MapLibre's offline regions. App-scoped so a download started on the
     * area screen keeps running after that screen is closed.
     */
    internal val offlineMaps: OfflineMaps by lazy { OfflineMaps(this) }

    private val defaultDb: Database by lazy {
        Database(
            driver = AndroidSQLiteDriver(),
            path = getDatabasePath(DATABASE_NAME).absolutePath,
        )
    }

    override fun onCreate() {
        super.onCreate()
        settingsInit(this)
        typefaceInit(this)
        MapLibre.getInstance(this)

        // OfflineManager must be created on the UI thread; touching the lazy
        // here does that before the background refresh below uses it.
        offlineMaps

        // Delete the databases abandoned by earlier versions and load the
        // settings from the database up front so later reads from the main
        // thread hit the in-memory cache. The session token is part of the
        // cache, so this also makes it available without touching the database.
        ioScope.launch {
            try {
                val path = getDatabasePath(DATABASE_NAME)
                path.parentFile?.let { LegacyDatabases.delete(it, path) }
                prefs.preload()
                // Re-attach to packs left incomplete by a previous run and
                // resume them.
                offlineMaps.refresh()
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
            }
        }
    }

    /**
     * Builds Coil's singleton loader with a listener that records load counts
     * for the image stats screen. Everything else keeps Coil's defaults,
     * including the memory and disk caches.
     */
    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .eventListenerFactory(ImageStatsEventListener.Factory)
            .build()
    }
}

fun Fragment.api(): Api = (requireContext().applicationContext as App).api

fun Fragment.db(): Database = (requireContext().applicationContext as App).db

fun Fragment.app(): App = requireContext().applicationContext as App

internal fun Fragment.syncController(): SyncController =
    (requireContext().applicationContext as App).syncController

internal fun Fragment.offlineMaps(): OfflineMaps =
    (requireContext().applicationContext as App).offlineMaps
