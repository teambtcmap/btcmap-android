package org.btcmap.search

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class NominatimSearchTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun searcher(minIntervalMs: Long = 1) = NominatimSearch(
        httpClient = HttpClient(CIO),
        baseUrl = serverRule.server.url("/search").toString(),
        minInterval = minIntervalMs.milliseconds,
    )

    private fun enqueue(body: String, code: Int = 200) {
        serverRule.server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    @Test
    fun search_parsesNameAndCoordinates() = runTest {
        enqueue(
            """
            [
                {"lat":"39.8983136","lon":"116.3883278","name":"致美楼饭庄","display_name":"致美楼饭庄, 北京市"},
                {"lat":"1.5","lon":"2.5","display_name":"12, Rue de la Paix, Paris"},
                {"lat":"3.5","lon":"4.5","name":null,"display_name":"Explicit null name"}
            ]
            """.trimIndent(),
        )

        val places = searcher().search("致美楼饭庄", limit = 3)

        Assert.assertEquals(3, places.size)
        Assert.assertEquals(NominatimPlace(39.8983136, 116.3883278, "致美楼饭庄"), places[0])
        // A feature with no name is still offered by its display name.
        Assert.assertEquals(NominatimPlace(1.5, 2.5, "12, Rue de la Paix, Paris"), places[1])
        // An explicit JSON null name must not become the literal "null".
        Assert.assertEquals(NominatimPlace(3.5, 4.5, "Explicit null name"), places[2])
    }

    @Test
    fun search_skipsRowsWithoutCoordinatesOrName() = runTest {
        enqueue(
            """
            [
                {"lat":"1.0","lon":"2.0"},
                {"lon":"2.0","name":"No latitude"},
                {"lat":"not-a-number","lon":"2.0","name":"Bad latitude"},
                {"lat":"3.0","lon":"4.0","name":"  "}
            ]
            """.trimIndent(),
        )

        Assert.assertTrue(searcher().search("q").isEmpty())
    }

    @Test
    fun search_returnsEmptyOnAnErrorStatus() = runTest {
        enqueue("rate limited", code = 429)

        Assert.assertTrue(searcher(minIntervalMs = 0).search("q").isEmpty())
    }

    @Test
    fun search_returnsEmptyOnAMalformedBody() = runTest {
        enqueue("not json at all")

        Assert.assertTrue(searcher().search("q").isEmpty())
    }

    @Test
    fun search_spacesRequestsByTheMinimumInterval() {
        // Real time, not runTest's virtual clock: the throttle paces with
        // kotlin.time and a coroutine delay, both of which runTest would skip.
        runBlocking {
            enqueue("[]")
            enqueue("[]")

            val searcher = searcher(minIntervalMs = 300)
            val start = TimeSource.Monotonic.markNow()
            searcher.search("first")
            searcher.search("second")
            val elapsed = start.elapsedNow()

            // The second request cannot start until a window after the first,
            // so two back-to-back calls take at least one interval.
            Assert.assertTrue(
                "two searches took $elapsed, expected at least 300ms",
                elapsed >= 300.milliseconds,
            )
        }
    }
}
