package org.btcmap.app

import android.app.Application
import androidx.fragment.app.Fragment
import org.btcmap.db.Database
import org.btcmap.db.DbHelper
import org.maplibre.android.MapLibre
import org.btcmap.settings.init as settingsInit
import org.btcmap.typeface.init as typefaceInit

class App : Application() {

    val db: Database by lazy { Database(DbHelper(this)) }

    override fun onCreate() {
        super.onCreate()
        settingsInit(this)
        typefaceInit(this)
        MapLibre.getInstance(this)
    }
}

fun Fragment.db(): Database = (requireContext().applicationContext as App).db