package org.btcmap.auth

import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.btcmap.api.Api
import org.btcmap.api.ApiTestBase
import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Test

class ApiAuthTest : ApiTestBase() {

    private fun okResponse() = MockResponse.Builder().body("ok").build()

    private fun api(
        base: () -> String = { server.url("/").toString() },
        token: () -> String?,
    ) = Api(
        httpClient = httpClient(),
        baseUrl = { base().toUrl() },
        token = token,
    )

    @Test
    fun addsStoredToken() = runTest {
        server.enqueue(okResponse())

        api { "token-1" }
            .call(HttpMethod.Get, url("/x")) { it }

        Assert.assertEquals("Bearer token-1", takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsBlankToken() = runTest {
        server.enqueue(okResponse())

        api { "  " }
            .call(HttpMethod.Get, url("/x")) { it }

        Assert.assertNull(takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsMissingToken() = runTest {
        server.enqueue(okResponse())

        api { null }
            .call(HttpMethod.Get, url("/x")) { it }

        Assert.assertNull(takeRequest().headers["Authorization"])
    }

    @Test
    fun doesNotOverrideExplicitAuthorization() = runTest {
        server.enqueue(okResponse())

        api { "stored-token" }
            .call(HttpMethod.Get, url("/x"), authorization = "Bearer password") { it }

        Assert.assertEquals("Bearer password", takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsPublicRequests() = runTest {
        server.enqueue(okResponse())

        api { "stored-token" }
            .call(HttpMethod.Get, url("/x"), withoutAuth = true) { it }

        Assert.assertNull(takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsForeignHost() = runTest {
        server.enqueue(okResponse())

        api(token = { "stored-token" }, base = { "https://api.btcmap.org" })
            .call(HttpMethod.Get, url("/x")) { it }

        Assert.assertNull(takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsSameHostOnDifferentPort() = runTest {
        server.enqueue(okResponse())

        val base = server.url("/")
        api(token = { "stored-token" }, base = { base.newBuilder().port(base.port + 1).build().toString() })
            .call(HttpMethod.Get, url("/x")) { it }

        Assert.assertNull(takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsSameHostOnDifferentScheme() = runTest {
        server.enqueue(okResponse())

        val base = server.url("/")
        api(token = { "stored-token" }, base = { base.newBuilder().scheme("https").build().toString() })
            .call(HttpMethod.Get, url("/x")) { it }

        Assert.assertNull(takeRequest().headers["Authorization"])
    }

    @Test
    fun skipsTokenWhenConfiguredApiUrlIsMalformed() = runTest {
        server.enqueue(okResponse())

        api(token = { "stored-token" }, base = { throw IllegalArgumentException("malformed URL") })
            .call(HttpMethod.Get, url("/x")) { it }

        Assert.assertNull(takeRequest().headers["Authorization"])
    }
}
