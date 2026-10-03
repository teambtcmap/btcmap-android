package org.btcmap.webspike

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

/** The Ktor client wiring, exercised on the JVM with a mock engine. */
class KtorClientTest {

    @Test
    fun fetchesWithKtorMockEngine() = runBlocking {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = """{"id":7,"name":"Ktor Cafe","updated_at":"2026-10-03T12:00:00"}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        try {
            val place = parsePlace(fetch(client, "https://api.btcmap.org/v4/places/7"))
            assertEquals(7, place.id)
            assertEquals("Ktor Cafe", place.name)
        } finally {
            client.close()
        }
    }
}
