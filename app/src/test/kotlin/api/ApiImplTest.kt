package api

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiImplTest {

    @Test
    fun getAreas() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("[]"))
        server.start()

        val api = ApiImpl(
            baseUrl = server.url(""),
            httpClient = OkHttpClient(),
        )

        assertEquals(0, api.getAreas(null, 100).size)
        server.shutdown()
    }

    @Test
    fun getElements() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("[]"))
        server.start()

        val api = ApiImpl(
            baseUrl = server.url(""),
            httpClient = OkHttpClient(),
        )

        assertEquals(0, api.getElements(null, 100).size)
        server.shutdown()
    }

    @Test
    fun getEvents() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("[]"))
        server.start()

        val api = ApiImpl(
            baseUrl = server.url(""),
            httpClient = OkHttpClient(),
        )

        assertEquals(0, api.getEvents(null, 100).size)
        server.shutdown()
    }

    @Test
    fun getReports() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("[]"))
        server.start()

        val api = ApiImpl(
            baseUrl = server.url(""),
            httpClient = OkHttpClient(),
        )

        assertEquals(0, api.getReports(null, 100).size)
        server.shutdown()
    }

    @Test
    fun getUsers() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("[]"))
        server.start()

        val api = ApiImpl(
            baseUrl = server.url(""),
            httpClient = OkHttpClient(),
        )

        assertEquals(0, api.getUsers(null, 100).size)
        server.shutdown()
    }
}