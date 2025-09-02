package app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import initDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        initDatabase()

        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
}