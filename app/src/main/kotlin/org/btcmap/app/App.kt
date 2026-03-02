package org.btcmap.app

import android.app.Application
import org.maplibre.android.MapLibre
import org.btcmap.db.init as dbInit
import org.btcmap.settings.init as settingsInit
import org.btcmap.typeface.init as typefaceInit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        settingsInit(this)
        dbInit(this)
        typefaceInit(this)
        MapLibre.getInstance(this)
    }
}
