package org.btcmap.auth

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import org.btcmap.settings.authToken

class TokenSettingInterceptor(private val prefs: SharedPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request().newBuilder()
                .apply {
                    val token = prefs.authToken
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                }
                .build()
        )
    }
}