package org.btcmap.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.junit4.MockWebServerRule
import org.btcmap.util.toUrl
import org.junit.Rule

abstract class ApiTestBase {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    protected val server: MockWebServer
        get() = serverRule.server

    /** The User-Agent/origin the test [api] reports to the server. */
    protected val userAgent = "test-user-agent"

    protected fun httpClient(): HttpClient = HttpClient(CIO)

    protected fun baseUrl(): Url = server.url("/").toString().toUrl()

    protected fun url(path: String): Url = server.url(path).toString().toUrl()

    protected fun api(): Api {
        return Api(
            httpClient = httpClient(),
            baseUrl = { baseUrl() },
            userAgent = userAgent,
        )
    }

    protected fun enqueueJson(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
        )
    }

    protected fun takeRequest(): RecordedRequest {
        return server.takeRequest()
    }

    protected fun RecordedRequest.jsonBody(): String {
        return body?.utf8() ?: ""
    }
}
