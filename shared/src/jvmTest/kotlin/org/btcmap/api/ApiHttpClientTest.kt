package org.btcmap.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.brotli.BrotliInterceptor
import org.btcmap.auth.TokenSettingInterceptor
import org.btcmap.http.RateLimitingInterceptor
import org.btcmap.http.UserAgentSettingInterceptor
import org.junit.Assert
import org.junit.Test

class ApiHttpClientTest {
    private fun client(userAgent: String = "test-agent"): okhttp3.OkHttpClient {
        return apiHttpClient(
            userAgent = userAgent,
            token = { null },
            apiUrl = { "https://example.com".toHttpUrl() },
        )
    }

    @Test
    fun apiHttpClient_installsInterceptorsInOrder() {
        val interceptors = client().interceptors.map { it::class.java }

        Assert.assertEquals(
            listOf(
                BrotliInterceptor::class.java,
                UserAgentSettingInterceptor::class.java,
                TokenSettingInterceptor::class.java,
                RateLimitingInterceptor::class.java,
            ),
            interceptors,
        )
    }

    @Test
    fun apiHttpClient_configuresTimeouts() {
        val client = client()

        Assert.assertEquals(15_000, client.connectTimeoutMillis)
        Assert.assertEquals(60_000, client.readTimeoutMillis)
        Assert.assertEquals(30_000, client.writeTimeoutMillis)
    }
}
