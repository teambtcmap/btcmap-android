package org.btcmap

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.btcmap.api.Api
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ApiTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun createApi(): Api {
        return Api(
            httpClient = OkHttpClient(),
            url = serverRule.server.url("/")
        )
    }

    @Test
    fun savePlace_success() = runTest {
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body("").build()

        serverRule.server.enqueue(response)

        api.savePlace(123)

        val request = serverRule.server.takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertTrue(request.requestLine.contains("/v4/places/saved"))
    }

    @Test(expected = Exception::class)
    fun savePlace_failure() = runTest {
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .status("HTTP/1.1 500 Internal Server Error")
            .body("").build()

        serverRule.server.enqueue(response)

        api.savePlace(123)
    }

    @Test
    fun removeSavedPlace_success() = runTest {
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body("").build()

        serverRule.server.enqueue(response)

        api.removeSavedPlace(123)

        val request = serverRule.server.takeRequest()
        Assert.assertEquals("DELETE", request.method)
        Assert.assertTrue(request.requestLine.contains("/v4/places/saved/123"))
    }

    @Test(expected = Exception::class)
    fun removeSavedPlace_failure() = runTest {
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .status("HTTP/1.1 500 Internal Server Error")
            .body("").build()

        serverRule.server.enqueue(response)

        api.removeSavedPlace(123)
    }

    @Test
    fun saveArea_success() = runTest {
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body("[123, 456, 789]").build()

        serverRule.server.enqueue(response)

        val result = api.saveArea(123)

        val request = serverRule.server.takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertTrue(request.requestLine.contains("/v4/areas/saved"))
        Assert.assertEquals(listOf(123L, 456L, 789L), result)
    }

    @Test(expected = Exception::class)
    fun saveArea_failure() = runTest {
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .status("HTTP/1.1 500 Internal Server Error")
            .body("").build()

        serverRule.server.enqueue(response)

        api.saveArea(123)
    }

    @Test
    fun removeSavedArea_success() = runTest {
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body("[456, 789]").build()

        serverRule.server.enqueue(response)

        val result = api.removeSavedArea(123)

        val request = serverRule.server.takeRequest()
        Assert.assertEquals("DELETE", request.method)
        Assert.assertTrue(request.requestLine.contains("/v4/areas/saved/123"))
        Assert.assertEquals(listOf(456L, 789L), result)
    }

    @Test(expected = Exception::class)
    fun removeSavedArea_failure() = runTest {
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .status("HTTP/1.1 500 Internal Server Error")
            .body("").build()

        serverRule.server.enqueue(response)

        api.removeSavedArea(123)
    }
}