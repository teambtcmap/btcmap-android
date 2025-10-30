package app

import android.app.Application
import org.maplibre.android.MapLibre

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        settings.init(this)
        db.init(this)
        typeface.init(this)
        MapLibre.getInstance(this)
    }
}