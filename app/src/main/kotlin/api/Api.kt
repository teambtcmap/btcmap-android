package api

import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import settings.apiUrl
import settings.prefs

fun apiUrl(endpoint: String) = prefs.apiUrl
    .newBuilder().apply {
        addPathSegment("v4")
        addPathSegment(endpoint)
    }

fun apiHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(BrotliInterceptor)
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