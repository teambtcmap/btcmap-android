package org.btcmap.app

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.sqlite.driver.AndroidSQLiteDriver
import org.btcmap.db.Database
import org.maplibre.android.MapLibre
import org.btcmap.settings.init as settingsInit
import org.btcmap.typeface.init as typefaceInit

class App : Application() {

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

fun Fragment.db(): Database = (requireContext().applicationContext as App).db