package org.btcmap.api

import okhttp3.brotli.BrotliInterceptor
import org.btcmap.auth.TokenSettingInterceptor
import org.btcmap.http.RateLimitingInterceptor
import org.btcmap.http.UserAgentSettingInterceptor
import org.junit.Assert
import org.junit.Test

class ApiHttpClientTest {
    @Test
    fun apiHttpClient_installsInterceptorsInOrder() {
        val interceptors = apiHttpClient().interceptors.map { it::class.java }

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
        val client = apiHttpClient()

        Assert.assertEquals(15_000, client.connectTimeoutMillis)
        Assert.assertEquals(60_000, client.readTimeoutMillis)
        Assert.assertEquals(30_000, client.writeTimeoutMillis)
    }
}
