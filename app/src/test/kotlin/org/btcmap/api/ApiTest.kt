package org.btcmap.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okhttp3.Request
import org.btcmap.util.toJsonObject
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class ApiTest : ApiTestBase() {
    @Test
    fun call_returnsParsedBodyOnSuccess() = runTest {
        enqueueJson("""{"value":42}""")

        val result = api().call(Request.Builder().url(server.url("/x")).build()) { stream ->
            stream.bufferedReader().readText()
        }

        Assert.assertEquals("""{"value":42}""", result)
    }

    @Test
    fun call_failureIncludesServerMessage() = runTest {
        enqueueJson("""{"message":"place is closed"}""", code = 400)

        try {
            api().call(Request.Builder().url(server.url("/x")).build()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(400, e.code)
            Assert.assertEquals("place is closed", e.message)
        }
    }

    @Test
    fun call_malformedJsonThrowsParseException() = runTest {
        enqueueJson("not json")

        try {
            api().call(Request.Builder().url(server.url("/x")).build()) { it.toJsonObject() }
            Assert.fail("Expected ApiParseException")
        } catch (e: ApiParseException) {
            Assert.assertNotNull(e.cause)
        }
    }

    @Test
    fun call_failureIncludesServerErrorCode() = runTest {
        enqueueJson("""{"code":"invalid_input","message":"days out of range"}""", code = 400)

        try {
            api().call(Request.Builder().url(server.url("/x")).build()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(400, e.code)
            Assert.assertEquals("invalid_input", e.errorCode)
            Assert.assertEquals("days out of range", e.message)
        }
    }

    @Test
    fun call_failureWithoutMessageFallsBackToStatusAndBody() = runTest {
        enqueueJson("boom", code = 500)

        try {
            api().call(Request.Builder().url(server.url("/x")).build()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(500, e.code)
            Assert.assertEquals("HTTP 500: boom", e.message)
        }
    }

    @Test
    fun call_emptyFailureBodyFallsBackToUnexpectedResponse() = runTest {
        enqueueJson("", code = 503)

        try {
            api().call(Request.Builder().url(server.url("/x")).build()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(503, e.code)
            Assert.assertEquals("HTTP 503: unexpected response", e.message)
        }
    }

    @Test
    fun call_invokesOnUnauthorizedOn401() = runTest {
        enqueueJson("""{"message":"Authentication required"}""", code = 401)

        var unauthorized = false
        val api = Api(
            httpClient = OkHttpClient(),
            baseUrl = { server.url("/") },
            onUnauthorized = { unauthorized = true },
        )

        try {
            api.call(authorizedRequest()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(401, e.code)
        }

        Assert.assertTrue(unauthorized)
    }

    @Test
    fun call_passesRejectedBearerTokenToOnUnauthorized() = runTest {
        enqueueJson("""{"message":"Authentication required"}""", code = 401)

        var rejectedToken: String? = null
        val api = Api(
            httpClient = OkHttpClient(),
            baseUrl = { server.url("/") },
            onUnauthorized = { rejectedToken = it },
        )

        try {
            api.call(authorizedRequest()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(401, e.code)
        }

        Assert.assertEquals("stale-token", rejectedToken)
    }

    @Test
    fun call_doesNotInvokeOnUnauthorizedWithoutAuthorizationHeader() = runTest {
        // A request sent without the stored token (e.g. because the keystore was
        // temporarily unavailable) must not clear a still-valid session.
        enqueueJson("""{"message":"Authentication required"}""", code = 401)

        var unauthorized = false
        val api = Api(
            httpClient = OkHttpClient(),
            baseUrl = { server.url("/") },
            onUnauthorized = { unauthorized = true },
        )

        try {
            api.call(Request.Builder().url(server.url("/x")).build()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(401, e.code)
        }

        Assert.assertFalse(unauthorized)
    }

    private fun authorizedRequest() = Request.Builder()
        .url(server.url("/x"))
        .header("Authorization", "Bearer stale-token")
        .build()

    @Test
    fun call_stillThrowsApiExceptionWhenOnUnauthorizedFails() = runTest {
        enqueueJson("""{"message":"Authentication required"}""", code = 401)

        val api = Api(
            httpClient = OkHttpClient(),
            baseUrl = { server.url("/") },
            onUnauthorized = { throw IllegalStateException("failed to clear session") },
        )

        try {
            api.call(authorizedRequest()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(401, e.code)
            Assert.assertTrue(e.suppressed.any { it is IllegalStateException })
        }
    }

    @Test
    fun call_wrapsConnectionFailureInTransportException() = runTest {
        val url = server.url("/x")
        server.close()

        val api = Api(httpClient = OkHttpClient(), baseUrl = { url })

        try {
            api.call(Request.Builder().url(url).build()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiTransportException")
        } catch (e: ApiTransportException) {
            Assert.assertTrue(e.cause is IOException)
        }
    }

    @Test
    fun call_wrapsTruncatedBodyInTransportException() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""{"value":42}""")
                .onResponseBody(SocketEffect.CloseStream())
                .build()
        )

        try {
            api().call(Request.Builder().url(server.url("/x")).build()) { it.toJsonObject() }
            Assert.fail("Expected ApiTransportException")
        } catch (e: ApiTransportException) {
            Assert.assertTrue("cause chain: ${e.causeChain()}", e.causeChain().any { it is IOException })
        }
    }

    @Test
    fun call_propagatesCancellationUntouched() = runTest {
        enqueueJson("""{"value":42}""")

        try {
            api().call(Request.Builder().url(server.url("/x")).build()) {
                throw CancellationException("cancelled")
            }
            Assert.fail("Expected CancellationException")
        } catch (e: CancellationException) {
            Assert.assertEquals("cancelled", e.message)
        }
    }

    @Test
    fun call_doesNotInvokeOnUnauthorizedOnOtherErrors() = runTest {
        enqueueJson("""{"message":"boom"}""", code = 500)

        var unauthorized = false
        val api = Api(
            httpClient = OkHttpClient(),
            baseUrl = { server.url("/") },
            onUnauthorized = { unauthorized = true },
        )

        try {
            api.call(Request.Builder().url(server.url("/x")).build()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(500, e.code)
        }

        Assert.assertFalse(unauthorized)
    }

    private fun Throwable.causeChain(): List<Throwable> =
        generateSequence(this) { it.cause }.toList()
}
