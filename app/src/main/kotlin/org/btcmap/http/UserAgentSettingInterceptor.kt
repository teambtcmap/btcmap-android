package org.btcmap.http

import okhttp3.Interceptor
import okhttp3.Response
import org.btcmap.BuildConfig

object UserAgentSettingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "BTC Map Android ${BuildConfig.VERSION_CODE}")
            .build()
        return chain.proceed(request)
    }
}