package org.btcmap.api

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ApiHttpClientTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    @Test
    fun apiHttpClient_sendsTheConfiguredUserAgent() = runBlocking {
        serverRule.server.enqueue(MockResponse.Builder().body("ok").build())

        apiHttpClient("test-agent")
            .get(serverRule.server.url("/x").toString().toUrl())
            .bodyAsText()

        Assert.assertEquals("test-agent", serverRule.server.takeRequest().headers["User-Agent"])
    }
}
