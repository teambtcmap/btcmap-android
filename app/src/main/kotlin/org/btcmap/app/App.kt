package org.btcmap.app

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.sqlite.driver.AndroidSQLiteDriver
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.btcmap.BuildConfig
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.btcmap.settings.apiUrl
import org.btcmap.settings.prefs
import org.maplibre.android.MapLibre
import org.btcmap.settings.init as settingsInit
import org.btcmap.typeface.init as typefaceInit

class App : Application() {
    val api: Api by lazy {
        Api(
            httpClient = apiHttpClient,
            url = prefs.apiUrl,
        )
    }

    private val apiHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(BrotliInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "BTC Map Android ${BuildConfig.VERSION_NAME}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor {
                var res = it.proceed(it.request())

                var retryAttempts = 0

                while (res.code == 429 && retryAttempts < 10) {
                    res.close()
                    Thread.sleep(retryAttempts * 1000 + (Math.random() * 1000.0).toLong())
                    res = it.proceed(it.request())
                    retryAttempts++
                }

                res
            }.build()
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

fun Fragment.api(): Api = (requireContext().applicationContext as App).api

fun Fragment.db(): Database = (requireContext().applicationContext as App).db