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
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    fun doesNotRetryWhenRetryAfterExceedsMaxDelay() {
        val server = server()
        server.enqueue(retryableResponse(retryAfter = "120"))

        client().newCall(Request.Builder().url(server.url("/x")).build()).execute().use {
            Assert.assertEquals(429, it.code)
        }

        Assert.assertEquals(1, server.requestCount)
    }

    @Test
    fun stopsRetryingWhenCallIsCanceled() {
        val server = server()
        server.enqueue(retryableResponse(retryAfter = "1"))
        server.enqueue(MockResponse.Builder().body("ok").build())

        val call = client().newCall(Request.Builder().url(server.url("/x")).build())
        val executor = Executors.newSingleThreadExecutor()
        try {
            val future = executor.submit(Callable { call.execute() })

            // Wait until the interceptor holds the 429, then cancel while it is
            // waiting out the Retry-After delay.
            server.takeRequest()
            call.cancel()

            val error = try {
                future.get(5, TimeUnit.SECONDS)
                null
            } catch (e: ExecutionException) {
                e.cause
            }

            Assert.assertTrue("expected IOException but got $error", error is IOException)
            Assert.assertEquals(1, server.requestCount)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun restoresInterruptFlagWhenInterruptedDuringRetryDelay() {
        val server = server()
        server.enqueue(retryableResponse(retryAfter = "10"))

        val call = client().newCall(Request.Builder().url(server.url("/x")).build())
        var error: Throwable? = null
        var interruptFlagRestored = false

        val thread = Thread {
            try {
                call.execute()
            } catch (e: Throwable) {
                error = e
            } finally {
                interruptFlagRestored = Thread.currentThread().isInterrupted
            }
        }

        try {
            thread.start()
            server.takeRequest()
            thread.interrupt()
            thread.join(5_000)
        } finally {
            call.cancel()
            thread.join(1_000)
        }

        Assert.assertTrue("expected IOException but got $error", error is IOException)
        Assert.assertTrue(interruptFlagRestored)
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
