package org.btcmap.auth

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

internal class TokenSettingInterceptor(
    private val token: () -> String?,
    private val apiUrl: () -> HttpUrl,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.header("Authorization") != null) {
            return chain.proceed(request)
        }

        if (request.tag(PublicRequest::class.java) != null) {
            return chain.proceed(request)
        }

        // Never attach the session token to a host other than the configured API.
        val api = apiUrl()
        if (request.url.scheme != api.scheme ||
            request.url.host != api.host ||
            request.url.port != api.port
        ) {
            return chain.proceed(request)
        }

        val token = token()?.takeIf { it.isNotBlank() }
            ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
