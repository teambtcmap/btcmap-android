package org.btcmap.area

import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.util.assertNoUncaughtException
import org.btcmap.util.waitUntil
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class AreaCancellationTest : AreaScreenTest() {

    @Test
    fun destroyingViewDuringLoad_doesNotRunErrorHandling() {
        val release = CountDownLatch(1)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                if (path == "/v4/areas/1") {
                    release.await()
                }
                return when {
                    path.endsWith("/events") -> jsonResponse(EMPTY_EVENTS_JSON)
                    path.startsWith("/v4/place-issues") -> jsonResponse(EMPTY_ISSUES_JSON)
                    else -> jsonResponse(areaJson())
                }
            }
        }

        try {
            withArea { scenario, _ ->
                waitUntil { apiRule.server.requestCount >= 1 }

                assertNoUncaughtException(
                    "Cancelled area load ran error handling after the view was destroyed",
                ) {
                    scenario.moveToState(Lifecycle.State.DESTROYED)
                }
            }
        } finally {
            release.countDown()
        }
    }
}
