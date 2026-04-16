package org.btcmap.api

import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.btcmap.auth.TokenSettingInterceptor
import org.btcmap.http.RateLimitingInterceptor
import org.btcmap.http.UserAgentSettingInterceptor
import org.btcmap.settings.prefs

fun apiHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(BrotliInterceptor)
        .addInterceptor(UserAgentSettingInterceptor)
        .addInterceptor(TokenSettingInterceptor(prefs))
        .addInterceptor(RateLimitingInterceptor)
        .build()
}