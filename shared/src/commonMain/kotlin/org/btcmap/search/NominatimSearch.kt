package org.btcmap.search

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.api.apiHttpClient
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.toJsonArray
import org.btcmap.util.toUrl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** A place the OpenStreetMap Nominatim search service found, by name. */
data class NominatimPlace(
    val lat: Double,
    val lon: Double,
    val name: String,
)

/**
 * Searches the public OpenStreetMap Nominatim service by name.
 *
 * Nominatim's usage policy allows at most one request per second, so calls are
 * serialised and spaced by at least [minInterval]: a burst of queries, as when
 * the search field changes, never exceeds the limit. The service only ever adds
 * to the local results, so a failure or a malformed response yields an empty
 * list rather than an error, and the caller keeps whatever it already found.
 *
 * The caller is expected to pass a client whose user agent identifies the app,
 * which the policy requires; [create] builds one.
 */
class NominatimSearch(
    private val httpClient: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val minInterval: Duration = 1.seconds,
) {
    private val throttle = Mutex()

    /** When the last request was issued, for pacing the next one. */
    private var lastRequest: TimeMark? = null

    suspend fun search(query: String, limit: Int = DEFAULT_LIMIT): List<NominatimPlace> =
        // The lock is held for the whole request, so requests are strictly
        // serialised and the gap between two starts is never below [minInterval].
        throttle.withLock {
            lastRequest?.let { issued ->
                val remaining = minInterval - issued.elapsedNow()
                if (remaining > Duration.ZERO) delay(remaining)
            }
            lastRequest = TimeSource.Monotonic.markNow()

            val url = URLBuilder(baseUrl.toUrl()).apply {
                parameters.append("q", query)
                parameters.append("format", "jsonv2")
                parameters.append("limit", limit.toString())
                // The result rows show only a name and a distance, so the
                // address breakdown would be parsed and thrown away.
                parameters.append("addressdetails", "0")
            }.build()

            try {
                val response = httpClient.get(url)
                if (!response.status.isSuccess()) {
                    emptyList()
                } else {
                    parse(response.bodyAsText())
                }
            } catch (t: Throwable) {
                // A transport failure is not worth surfacing: the group is
                // best-effort extras beside the local results.
                t.rethrowIfCancellation()
                emptyList()
            }
        }

    private fun parse(body: String): List<NominatimPlace> = runCatching {
        body.toJsonArray().mapNotNull { place ->
            val lat = place["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                ?: return@mapNotNull null
            val lon = place["lon"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                ?: return@mapNotNull null
            // A named feature carries `name`; an address-only match (a house
            // number, a street) carries only `display_name`, which is still
            // worth offering as a jump target.
            val name = place["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: place["display_name"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            NominatimPlace(lat = lat, lon = lon, name = name)
        }
    }.getOrDefault(emptyList())

    companion object {
        const val DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org/search"

        /** How many named places a single search offers. */
        const val DEFAULT_LIMIT = 5

        /**
         * A searcher with its own client, carrying a user agent that identifies
         * the app as Nominatim's policy requires.
         */
        fun create(userAgent: String): NominatimSearch =
            NominatimSearch(apiHttpClient(userAgent))
    }
}
