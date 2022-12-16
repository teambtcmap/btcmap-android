package app

import android.app.Application
import db.persistentDatabase
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.btcmap.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.DEBUG)
            androidContext(this@App)
            defaultModule()

            modules(module {
                single { persistentDatabase(this@App) }
                single { OkHttpClient.Builder().addInterceptor(BrotliInterceptor).build() }
                single { Json { ignoreUnknownKeys = true } }
            })
        }
    }
}