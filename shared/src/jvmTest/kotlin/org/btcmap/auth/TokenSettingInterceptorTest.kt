package org.btcmap.auth

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
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

    private fun client(
        apiUrl: () -> HttpUrl = { server().url("/") },
        token: () -> String?,
    ) = OkHttpClient.Builder()
        .addInterceptor(TokenSettingInterceptor(token, apiUrl))
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

    @Test
    fun skipsPublicRequests() {
        server().enqueue(okResponse())

        client { "stored-token" }
            .newCall(
                Request.Builder()
                    .url(server().url("/x"))
                    .withoutAuth()
                    .build()
            )
            .execute()
            .close()

        Assert.assertNull(server().takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsForeignHost() {
        server().enqueue(okResponse())

        client(token = { "stored-token" }, apiUrl = { "https://api.btcmap.org".toHttpUrl() })
            .newCall(Request.Builder().url(server().url("/x")).build())
            .execute()
            .close()

        Assert.assertNull(server().takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsSameHostOnDifferentPort() {
        server().enqueue(okResponse())

        val base = server().url("/")
        client(
            token = { "stored-token" },
            apiUrl = { base.newBuilder().port(base.port + 1).build() },
        )
            .newCall(Request.Builder().url(server().url("/x")).build())
            .execute()
            .close()

        Assert.assertNull(server().takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsSameHostOnDifferentScheme() {
        server().enqueue(okResponse())

        val base = server().url("/")
        client(
            token = { "stored-token" },
            apiUrl = { base.newBuilder().scheme("https").build() },
        )
            .newCall(Request.Builder().url(server().url("/x")).build())
            .execute()
            .close()

        Assert.assertNull(server().takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsTokenWhenConfiguredApiUrlIsMalformed() {
        server().enqueue(okResponse())

        client(
            token = { "stored-token" },
            apiUrl = { throw IllegalArgumentException("malformed URL") },
        )
            .newCall(Request.Builder().url(server().url("/x")).build())
            .execute()
            .close()

        Assert.assertNull(server().takeRequest().headers["Authorization"])
    }

    @Test
    fun dropsTokenWhenRedirectedToAnotherHost() {
        val other = MockWebServer()
        other.start()
        try {
            // The API answers with a redirect to a different host. The token was
            // attached for the API host only, so it must not follow the redirect.
            server().enqueue(
                MockResponse.Builder()
                    .code(302)
                    .addHeader("Location", other.url("/x").toString())
                    .build()
            )
            other.enqueue(okResponse())

            client { "stored-token" }
                .newCall(Request.Builder().url(server().url("/x")).build())
                .execute()
                .close()

            Assert.assertNull(other.takeRequest().headers["Authorization"])
        } finally {
            other.close()
        }
    }
}
