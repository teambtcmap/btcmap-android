package org.btcmap.http

import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.btcmap.BuildConfig

val httpClient by lazy {
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