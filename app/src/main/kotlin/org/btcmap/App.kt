package org.btcmap

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.sqlite.driver.AndroidSQLiteDriver
import coil3.ImageLoader
import coil3.SingletonImageLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import okio.Source
import okio.source
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.api.signOut
import org.btcmap.bundle.BundledAreas
import org.btcmap.bundle.BundledComments
import org.btcmap.bundle.BundledEvents
import org.btcmap.bundle.BundledPlaces
import org.btcmap.db.Database
import org.btcmap.db.LegacyDatabases
import org.btcmap.io.platformFileSystem
import org.btcmap.ui.ImageStatsEventListener
import org.btcmap.ui.map.OfflinePacks
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authToken
import org.btcmap.settings.prefs
import org.btcmap.sync.Sync
import org.btcmap.sync.SyncController
import org.btcmap.sync.SyncManager
import org.btcmap.ui.map.configureBundledMapResources
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.settings.init as settingsInit
import org.btcmap.util.initIconTypeface

private const val DATABASE_NAME = "btcmap.db"

class App : Application(), SingletonImageLoader.Factory {
    internal var apiForTesting: Api? = null

    internal var dbForTesting: Database? = null

    internal var mapStyleUriForTesting: String? = null

    /**
     * Completed once the database has been opened (migrating it first when
     * needed) and the settings loaded. [Activity] holds the first screen back
     * until then, so no screen opens or migrates the database on the main
     * thread.
     */
    internal val databaseReady = CompletableDeferred<Unit>()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sync: Sync
        get() = Sync(api, db)

    /**
     * The sync manager the screens observe. The app-scoped default seeds the
     * database from the bundled snapshots. The seeders live in `:shared`; only
     * opening the bundled assets is Android-specific, and [SyncManager] itself
     * only sees the seed results.
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
            seedPlaces = { onBatch ->
                BundledPlaces.import(db, onBatch) { openBundledSnapshot(BundledPlaces.FILE_NAME) }
                    .placesImported
            },
            seedEvents = {
                BundledEvents.import(db) { openBundledSnapshot(BundledEvents.FILE_NAME) }.eventsImported
            },
            seedComments = {
                BundledComments.import(db) { openBundledSnapshot(BundledComments.FILE_NAME) }
                    .commentsImported
            },
            seedAreas = {
                BundledAreas.import(db) { openBundledSnapshot(BundledAreas.FILE_NAME) }.areasImported
            },
        )
    }

    /**
     * Opens a bundled snapshot asset, or returns null when it is absent. The
     * snapshot is an optional offline fallback, so a missing asset is not an
     * error.
     */
    internal fun openBundledSnapshot(name: String): Source? =
        try {
            assets.open(name).source()
        } catch (_: java.io.IOException) {
            null
        }

    val api: Api
        get() = apiForTesting ?: defaultApi

    private val defaultApi: Api by lazy {
        Api(
            httpClient = apiHttpClient(userAgent),
            baseUrl = { prefs.apiUrl },
            token = { prefs.authToken },
            onUnauthorized = { handleUnauthorized(it) },
            userAgent = userAgent,
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
     * area screen keeps running after that screen is closed. Built lazily and on
     * first use (a screen's main thread), because it is created through the
     * Compose map's runtime.
     */
    internal val offlinePacks: OfflinePacks by lazy { OfflinePacks(resources.displayMetrics.density) }

    private val defaultDb: Database by lazy {
        Database(
            driver = AndroidSQLiteDriver(),
            path = getDatabasePath(DATABASE_NAME).absolutePath,
        )
    }

    override fun onCreate() {
        super.onCreate()
        settingsInit(this)
        // The icon font is loaded off the main thread (see initIconTypeface):
        // building it here would add a few tens of milliseconds to every cold
        // start before the first frame.
        initIconTypeface(this, ioScope)
        // The shared (Compose) map reads the bundled styles and their sprites
        // and glyphs through its own resource provider.
        configureBundledMapResources(this)

        // Load the settings before the first screen can read them: the session
        // token lives in this in-memory cache, so a read that raced the load
        // would report a signed-in user as signed out. Opening an up-to-date
        // database is cheap and happens here; an in-place migration can rewrite
        // whole tables, so that case runs on [ioScope] and [databaseReady] holds
        // the first screen back instead of blocking the main thread.
        val databasePath = getDatabasePath(DATABASE_NAME)
        if (Database.needsMigration(AndroidSQLiteDriver(), databasePath.absolutePath)) {
            ioScope.launch {
                preloadSettings()
                databaseReady.complete(Unit)
            }
        } else {
            preloadSettings()
            databaseReady.complete(Unit)
        }

        // Start the sync as soon as the database is usable instead of waiting
        // for the map's first resume. On a fresh install the bundled places
        // import runs in one transaction for the better part of a second, and
        // the map cannot draw a single pin until it commits, so overlapping the
        // import with the rest of startup is what gets the first pins on screen
        // soonest. The map still calls start() as well; a running sync is left
        // alone.
        ioScope.launch {
            databaseReady.await()
            syncController.start()
        }

        // Delete the databases abandoned by earlier versions. It is not needed
        // before the first screen, so it stays off the main thread.
        ioScope.launch {
            try {
                val fileSystem = platformFileSystem
                val legacyPath = databasePath.absolutePath.toPath()
                val parent = legacyPath.parent
                if (fileSystem != null && parent != null) {
                    LegacyDatabases.delete(fileSystem, parent, legacyPath)
                }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
            }
        }
    }

    /**
     * Loads the stored settings into the in-memory cache, so the session token
     * is available without touching the database. A failure leaves the cache
     * empty (the user reads as signed out) instead of crashing the start.
     */
    private fun preloadSettings() {
        try {
            prefs.preload()
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
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

internal fun Fragment.offlinePacks(): OfflinePacks =
    (requireContext().applicationContext as App).offlinePacks
