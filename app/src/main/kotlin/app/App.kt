package app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import db.persistentDatabase
import org.btcmap.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.DEBUG)

            androidContext(this@App)

            modules(
                appModule,
                module { single { persistentDatabase(this@App) } },
            )
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .respectCacheHeaders(false) // TODO remove after the server fixes its cache headers
            .build()
    }
}