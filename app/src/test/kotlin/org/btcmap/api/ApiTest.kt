package org.btcmap.api

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.junit.Test

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
            api.call(Request.Builder().url(server.url("/x")).build()) { it.bufferedReader().readText() }
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(401, e.code)
        }

        Assert.assertTrue(unauthorized)
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
}
