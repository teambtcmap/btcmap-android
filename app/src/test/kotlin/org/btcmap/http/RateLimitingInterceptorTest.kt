package org.btcmap.http

import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RateLimitingInterceptorTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun server() = serverRule.server

    private fun client(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RateLimitingInterceptor)
            .build()
    }

    private fun retryableResponse(retryAfter: String? = null): MockResponse {
        return MockResponse.Builder()
            .code(429)
            .apply { retryAfter?.let { addHeader("Retry-After", it) } }
            .build()
    }

    @Test
    fun retriesIdempotentRequestAfter429() {
        val server = server()
        server.enqueue(retryableResponse())
        server.enqueue(MockResponse.Builder().body("ok").build())

        client().newCall(Request.Builder().url(server.url("/x")).build()).execute().use {
            Assert.assertEquals(200, it.code)
        }

        Assert.assertEquals(2, server.requestCount)
    }

    @Test
    fun stopsAfterMaxRetryAttempts() {
        val server = server()
        repeat(11) { server.enqueue(retryableResponse(retryAfter = "0")) }

        client().newCall(Request.Builder().url(server.url("/x")).build()).execute().use {
            Assert.assertEquals(429, it.code)
        }

        Assert.assertEquals(11, server.requestCount)
    }

    @Test
    fun doesNotRetryNonIdempotentRequest() {
        val server = server()
        server.enqueue(retryableResponse())

        client().newCall(
            Request.Builder()
                .url(server.url("/x"))
                .post("".toRequestBody())
                .build()
        ).execute().use {
            Assert.assertEquals(429, it.code)
        }

        Assert.assertEquals(1, server.requestCount)
    }

    @Test
    fun retryAfterSecondsIsConvertedToMillis() {
        Assert.assertEquals(2_000L, responseWithRetryAfter("2").retryAfterMillis())
    }

    @Test
    fun retryAfterPastHttpDateIsZero() {
        val past = ZonedDateTime.now().minusHours(1).format(DateTimeFormatter.RFC_1123_DATE_TIME)

        Assert.assertEquals(0L, responseWithRetryAfter(past).retryAfterMillis())
    }

    @Test
    fun retryAfterIsNullWhenAbsentOrInvalid() {
        Assert.assertNull(responseWithRetryAfter(null).retryAfterMillis())
        Assert.assertNull(responseWithRetryAfter("not-a-date").retryAfterMillis())
    }

    private fun responseWithRetryAfter(value: String?): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(429)
            .message("Too Many Requests")
            .apply { value?.let { header("Retry-After", it) } }
            .body("".toResponseBody())
            .build()
    }
}
