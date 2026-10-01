package org.btcmap.feed

import android.os.Bundle
import androidx.compose.ui.test.junit4.createEmptyComposeRule
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
import org.btcmap.ui.ActivityFeedComposeView
import org.btcmap.ui.FEED_RETRY_TAG
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
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
                lateinit var feed: TestActivityFeedTab
                scenario.onActivity { activity ->
                    feed = TestActivityFeedTab().apply {
                        arguments = Bundle().apply {
                            putBoolean(BaseActivityFeedTab.ARG_SHOW_AREA_CHIPS, true)
                            putStringArrayList(
                                BaseActivityFeedTab.ARG_INITIAL_AREA_IDS,
                                arrayListOf("1"),
                            )
                            putStringArrayList(
                                BaseActivityFeedTab.ARG_INITIAL_AREA_NAMES,
                                arrayListOf("Area"),
                            )
                            putStringArrayList(
                                BaseActivityFeedTab.ARG_INITIAL_AREA_TYPES,
                                arrayListOf("community"),
                            )
                        }
                    }
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, feed, FEED_TAG)
                    }
                }
                waitUntil { apiRule.server.requestCount >= 1 }
                waitUntilOnMain {
                    !feed.requireView().findViewById<ActivityFeedComposeView>(R.id.feedList).loading
                }

                scenario.onActivity {
                    val message = feed.requireView()
                        .findViewById<ActivityFeedComposeView>(R.id.feedList)
                        .emptyMessage
                    Assert.assertNotEquals(
                        "A failed load must not be presented as the empty state",
                        feed.emptyMessage(),
                        message,
                    )
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
                lateinit var feed: TestActivityFeedTab
                scenario.onActivity { activity ->
                    feed = TestActivityFeedTab().apply {
                        arguments = Bundle().apply {
                            putBoolean(BaseActivityFeedTab.ARG_SHOW_AREA_CHIPS, true)
                            putStringArrayList(
                                BaseActivityFeedTab.ARG_INITIAL_AREA_IDS,
                                arrayListOf("1"),
                            )
                            putStringArrayList(
                                BaseActivityFeedTab.ARG_INITIAL_AREA_NAMES,
                                arrayListOf("Area"),
                            )
                            putStringArrayList(
                                BaseActivityFeedTab.ARG_INITIAL_AREA_TYPES,
                                arrayListOf("community"),
                            )
                        }
                    }
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, feed, FEED_TAG)
                    }
                }

                waitUntilOnMain {
                    feed.requireView().findViewById<ActivityFeedComposeView>(R.id.feedList)
                        .emptyMessage?.isNotBlank() == true
                }

                composeTestRule.onNodeWithTag(FEED_RETRY_TAG).performClick()

                waitUntilOnMain {
                    feed.requireView().findViewById<ActivityFeedComposeView>(R.id.feedList)
                        .emptyMessage == feed.emptyMessage()
                }
                Assert.assertEquals(2, attempts.get())
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    companion object {
        private const val FEED_TAG = "feed"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
