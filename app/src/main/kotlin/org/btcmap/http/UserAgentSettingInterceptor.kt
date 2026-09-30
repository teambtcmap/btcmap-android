package org.btcmap.http

import okhttp3.Interceptor
import okhttp3.Response
import org.btcmap.userAgent

object UserAgentSettingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(request)
    }
}