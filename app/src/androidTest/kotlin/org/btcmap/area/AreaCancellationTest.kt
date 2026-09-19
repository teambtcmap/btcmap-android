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
    fun destroyingViewDuringIssuesLoad_doesNotRunErrorHandling() {
        databaseRule.db.area.insert(listOf(area()))
        val release = CountDownLatch(1)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path.startsWith("/v4/place-issues") -> {
                        release.await()
                        jsonResponse(EMPTY_ISSUES_JSON)
                    }

                    else -> jsonResponse("[]")
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
