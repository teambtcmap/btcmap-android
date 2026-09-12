package org.btcmap.api

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.junit.Rule

abstract class ApiTestBase {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    protected val server: MockWebServer
        get() = serverRule.server

    protected fun api(): Api {
        return Api(
            httpClient = OkHttpClient(),
            url = server.url("/"),
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
