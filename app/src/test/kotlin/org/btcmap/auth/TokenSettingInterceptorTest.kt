package org.btcmap.auth

import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class TokenSettingInterceptorTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun server() = serverRule.server

    private fun client(token: () -> String?) = OkHttpClient.Builder()
        .addInterceptor(TokenSettingInterceptor(token))
        .build()

    private fun okResponse() = MockResponse.Builder().body("ok").build()

    @Test
    fun addsStoredToken() {
        server().enqueue(okResponse())

        client { "token-1" }
            .newCall(Request.Builder().url(server().url("/x")).build())
            .execute()
            .close()

        Assert.assertEquals("Bearer token-1", server().takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsBlankToken() {
        server().enqueue(okResponse())

        client { "  " }
            .newCall(Request.Builder().url(server().url("/x")).build())
            .execute()
            .close()

        Assert.assertNull(server().takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsMissingToken() {
        server().enqueue(okResponse())

        client { null }
            .newCall(Request.Builder().url(server().url("/x")).build())
            .execute()
            .close()

        Assert.assertNull(server().takeRequest().headers["Authorization"])
    }

    @Test
    fun doesNotOverrideExplicitAuthorization() {
        server().enqueue(okResponse())

        client { "stored-token" }
            .newCall(
                Request.Builder()
                    .url(server().url("/x"))
                    .header("Authorization", "Bearer password")
                    .build()
            )
            .execute()
            .close()

        Assert.assertEquals("Bearer password", server().takeRequest().headers["Authorization"])
    }
}
