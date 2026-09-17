package org.btcmap.api

import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.btcmap.auth.TokenSettingInterceptor
import org.btcmap.http.RateLimitingInterceptor
import org.btcmap.http.UserAgentSettingInterceptor
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authToken
import org.btcmap.settings.prefs
import java.util.concurrent.TimeUnit

fun apiHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(BrotliInterceptor)
        .addInterceptor(UserAgentSettingInterceptor)
        .addInterceptor(TokenSettingInterceptor(token = { prefs.authToken }, apiUrl = { prefs.apiUrl }))
        .addInterceptor(RateLimitingInterceptor)
        .build()
}
