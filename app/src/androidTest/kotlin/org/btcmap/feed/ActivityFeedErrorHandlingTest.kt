package org.btcmap.feed

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
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
import org.btcmap.ui.FEED_RETRY_TAG
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ActivityFeedErrorHandlingTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun loadFailure_isNotShownAsEmptyState() {
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val isActivity = request.url.encodedPath == "/v4/activity"
                return MockResponse.Builder()
                    .code(if (isActivity) 500 else 200)
                    .addHeader("Content-Type", "application/json")
                    .body(if (isActivity) """{"message":"boom"}""" else "[]")
                    .build()
            }
        }

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { showFeed(it) }
                waitUntil { apiRule.server.requestCount >= 1 }

                // The retry affordance only exists on the error state; the empty
                // state is not retryable, so a failed load must show it.
                composeTestRule.waitUntil(5_000) {
                    composeTestRule.onAllNodesWithTag(FEED_RETRY_TAG)
                        .fetchSemanticsNodes().isNotEmpty()
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    @Test
    fun loadFailure_tapToRetry_reloads() {
        val attempts = AtomicInteger()
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val isActivity = request.url.encodedPath == "/v4/activity"
                val fails = isActivity && attempts.getAndIncrement() == 0
                return MockResponse.Builder()
                    .code(if (fails) 500 else 200)
                    .addHeader("Content-Type", "application/json")
                    .body(if (fails) """{"message":"boom"}""" else "[]")
                    .build()
            }
        }

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { showFeed(it) }

                composeTestRule.waitUntil(5_000) {
                    composeTestRule.onAllNodesWithTag(FEED_RETRY_TAG)
                        .fetchSemanticsNodes().isNotEmpty()
                }

                composeTestRule.onNodeWithTag(FEED_RETRY_TAG).performClick()

                // The retry succeeds with an empty list, so the empty state shows.
                val empty = app.getString(R.string.activity_empty_local)
                composeTestRule.waitUntil(5_000) {
                    composeTestRule.onAllNodesWithText(empty)
                        .fetchSemanticsNodes().isNotEmpty()
                }
                Assert.assertEquals(2, attempts.get())
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun showFeed(activity: Activity) {
        activity.supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainerView, AppRootFragment.create(FEED_ROUTE), FEED_TAG)
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
