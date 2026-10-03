package org.btcmap.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

/**
 * Builds the shared HTTP client.
 *
 * The session token and the API URL are not baked in here: [Api] attaches the
 * token per request, so it can tell whether a rejected request actually carried
 * it. This client only carries the transport settings that are the same for
 * every call.
 */
fun apiHttpClient(userAgent: String): HttpClient {
    return HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        install(DefaultRequest) {
            header(HttpHeaders.UserAgent, userAgent)
        }
    }
}
