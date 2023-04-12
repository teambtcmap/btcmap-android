package api

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class ApiImplTest {

    @Test
    fun getAreas() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("[]"))
        server.start()

        val api = ApiImpl(
            baseUrl = server.url("/v2/"),
            httpClient = OkHttpClient(),
            json = Json { namingStrategy = JsonNamingStrategy.SnakeCase },
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
            baseUrl = server.url("/v2/"),
            httpClient = OkHttpClient(),
            json = Json { namingStrategy = JsonNamingStrategy.SnakeCase },
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
            baseUrl = server.url("/v2/"),
            httpClient = OkHttpClient(),
            json = Json { namingStrategy = JsonNamingStrategy.SnakeCase },
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
            baseUrl = server.url("/v2/"),
            httpClient = OkHttpClient(),
            json = Json { namingStrategy = JsonNamingStrategy.SnakeCase },
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
            baseUrl = server.url("/v2/"),
            httpClient = OkHttpClient(),
            json = Json { namingStrategy = JsonNamingStrategy.SnakeCase },
        )

        assertEquals(0, api.getUsers(null, 100).size)
        server.shutdown()
    }
}