package org.btcmap.feed

import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.nav.AppRootFragment
import org.btcmap.ui.AppRoute
import org.btcmap.util.AppTestCase
import org.btcmap.util.assertNoUncaughtException
import org.btcmap.util.waitUntil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class ActivityFeedCancellationTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun destroyingViewDuringLoad_doesNotRunErrorHandling() {
        val release = CountDownLatch(1)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.url.encodedPath == "/v4/activity") {
                    release.await()
                }
                return MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("[]")
                    .build()
            }
        }

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(
                            R.id.fragmentContainerView,
                            AppRootFragment.create(FEED_ROUTE),
                            FEED_TAG,
                        )
                    }
                }
                waitUntil { apiRule.server.requestCount >= 1 }

                assertNoUncaughtException(
                    "Cancelled activity feed load ran error handling after the view was destroyed",
                ) {
                    scenario.moveToState(Lifecycle.State.DESTROYED)
                }
            }
        } finally {
            release.countDown()
            app.mapStyleUriForTesting = null
        }
    }

    companion object {
        private val FEED_ROUTE = AppRoute.Feed(
            areaIds = listOf("1"),
            areaNames = listOf("Area"),
            areaTypes = listOf("community"),
        )
        private const val FEED_TAG = "feed"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
