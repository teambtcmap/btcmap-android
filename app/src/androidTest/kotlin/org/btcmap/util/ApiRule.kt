package org.btcmap.util

import androidx.test.core.app.ApplicationProvider
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.btcmap.App
import org.btcmap.api.Api
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ApiRule : TestRule {

    private val app = ApplicationProvider.getApplicationContext<App>()

    val server = MockWebServer()

    val api: Api
        get() = Api(OkHttpClient(), server.url("/"))

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                server.dispatcher = object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        return MockResponse.Builder()
                            .code(200)
                            .addHeader("Content-Type", "application/json")
                            .body("[]")
                            .build()
                    }
                }
                server.start()
                app.apiForTesting = api
                try {
                    base.evaluate()
                } finally {
                    app.apiForTesting = null
                    server.close()
                }
            }
        }
    }
}
