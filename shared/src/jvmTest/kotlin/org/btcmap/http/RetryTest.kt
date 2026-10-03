package org.btcmap.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RetryTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun server() = serverRule.server

    private fun client() = HttpClient(CIO)

    private fun url(path: String) = server().url(path).toString().toUrl()

    private fun retryableResponse(retryAfter: String? = null): MockResponse {
        return MockResponse.Builder()
            .code(429)
            .apply { retryAfter?.let { addHeader("Retry-After", it) } }
            .build()
    }

    @Test
    fun retriesIdempotentRequestAfter429() = runBlocking {
        server().enqueue(retryableResponse())
        server().enqueue(MockResponse.Builder().body("ok").build())

        val response = client().executeIdempotent(HttpMethod.Get, url("/x"))

        Assert.assertEquals(200, response.status.value)
        Assert.assertEquals(2, server().requestCount)
    }

    @Test
    fun stopsAfterMaxRetryAttempts() = runBlocking {
        repeat(11) { server().enqueue(retryableResponse(retryAfter = "0")) }

        val response = client().executeIdempotent(HttpMethod.Get, url("/x"))

        Assert.assertEquals(429, response.status.value)
        Assert.assertEquals(11, server().requestCount)
    }

    @Test
    fun doesNotRetryNonIdempotentRequest() = runBlocking {
        server().enqueue(retryableResponse())

        val response = client().executeIdempotent(HttpMethod.Post, url("/x"))

        Assert.assertEquals(429, response.status.value)
        Assert.assertEquals(1, server().requestCount)
    }

    @Test
    fun doesNotRetryWhenRetryAfterExceedsMaxDelay() = runBlocking {
        server().enqueue(retryableResponse(retryAfter = "120"))

        val response = client().executeIdempotent(HttpMethod.Get, url("/x"))

        Assert.assertEquals(429, response.status.value)
        Assert.assertEquals(1, server().requestCount)
    }

    @Test
    fun stopsRetryingWhenTheCallerIsCancelled() = runBlocking {
        server().enqueue(retryableResponse(retryAfter = "30"))
        server().enqueue(MockResponse.Builder().body("ok").build())

        val job = launch(Dispatchers.IO) {
            client().executeIdempotent(HttpMethod.Get, url("/x"))
        }

        // Wait until the first 429 arrives, then cancel while the retry waits.
        server().takeRequest()
        job.cancelAndJoin()

        Assert.assertEquals(1, server().requestCount)
    }

    @Test
    fun retryAfterSecondsIsConvertedToMillis() {
        Assert.assertEquals(2_000L, headersOf(HttpHeaders.RetryAfter, "2").retryAfterMillis())
    }

    @Test
    fun retryAfterPastHttpDateIsZero() {
        val past = ZonedDateTime.now().minusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME)

        Assert.assertEquals(0L, headersOf(HttpHeaders.RetryAfter, past).retryAfterMillis())
    }

    @Test
    fun retryAfterIsNullWhenAbsentOrInvalid() {
        Assert.assertNull(headersOf().retryAfterMillis())
        Assert.assertNull(headersOf(HttpHeaders.RetryAfter, "not-a-date").retryAfterMillis())
    }
}
