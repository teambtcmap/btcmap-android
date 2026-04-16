package org.btcmap

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.sqlite.driver.AndroidSQLiteDriver
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.db.Database
import org.btcmap.settings.apiUrl
import org.btcmap.settings.prefs
import org.maplibre.android.MapLibre
import org.btcmap.settings.init as settingsInit
import org.btcmap.util.init as typefaceInit

class App : Application() {
    val sync: Sync by lazy {
        Sync(api, db)
    }

    val api: Api by lazy {
        Api(
            httpClient = apiHttpClient(),
            url = prefs.apiUrl,
        )
    }

    val db: Database by lazy {
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
    }
}

fun Fragment.sync(): Sync = (requireContext().applicationContext as App).sync

fun Fragment.api(): Api = (requireContext().applicationContext as App).api

fun Fragment.db(): Database = (requireContext().applicationContext as App).db