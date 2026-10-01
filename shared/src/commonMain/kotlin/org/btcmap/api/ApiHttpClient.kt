package org.btcmap.api

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.btcmap.auth.TokenSettingInterceptor
import org.btcmap.http.RateLimitingInterceptor
import org.btcmap.http.UserAgentSettingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Builds the shared HTTP client. The session token and API URL are passed in as
 * providers because they live in the platform's settings store.
 */
fun apiHttpClient(
    userAgent: String,
    token: () -> String?,
    apiUrl: () -> HttpUrl,
): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(BrotliInterceptor)
        .addInterceptor(UserAgentSettingInterceptor(userAgent))
        .addInterceptor(TokenSettingInterceptor(token = token, apiUrl = apiUrl))
        .addInterceptor(RateLimitingInterceptor)
        .build()
}
