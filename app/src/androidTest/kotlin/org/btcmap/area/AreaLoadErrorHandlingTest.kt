package org.btcmap.area

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class AreaLoadErrorHandlingTest : AreaScreenTest() {

    @Test
    fun loadSuccess_hidesLoadingIndicatorAndShowsContent() {
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

        withArea { scenario, area ->
            waitUntil { apiRule.server.requestCount >= 1 }

            scenario.onActivity {
                val loading = area.requireView().findViewById<View>(R.id.loading)
                val content = area.requireView().findViewById<View>(R.id.content)
                Assert.assertTrue("Loading indicator should be visible", loading.isVisible)
                Assert.assertFalse("Content should be hidden while loading", content.isVisible)
            }

            release.countDown()

            waitUntilOnMain {
                !area.requireView().findViewById<View>(R.id.loading).isVisible
            }
            scenario.onActivity {
                val content = area.requireView().findViewById<View>(R.id.content)
                val title = area.requireView().findViewById<Toolbar>(R.id.toolbar).title
                Assert.assertTrue("Content should be visible after loading", content.isVisible)
                Assert.assertEquals("Grand Paris", title)
            }
        }
    }

    @Test
    fun loadFailure_showsErrorDialogWithDetails_andClosesScreenOnDismiss() {
        val release = CountDownLatch(1)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val isArea = request.url.encodedPath == "/v4/areas/1"
                if (isArea) {
                    release.await()
                }
                return if (isArea) {
                    jsonResponse("""{"message":"boom"}""", code = 500)
                } else {
                    jsonResponse("[]")
                }
            }
        }

        withArea(addToBackStack = true) { scenario, area ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            waitUntil { apiRule.server.requestCount >= 1 }

            scenario.onActivity {
                val loading = area.requireView().findViewById<View>(R.id.loading)
                Assert.assertTrue("Loading indicator should be visible", loading.isVisible)
            }

            release.countDown()

            waitUntil {
                runCatching {
                    onView(withText("boom")).inRoot(isDialog())
                        .check(matches(isDisplayed()))
                }.isSuccess
            }

            waitUntil {
                runCatching {
                    onView(withText(android.R.string.ok)).inRoot(isDialog())
                        .check(matches(isDisplayed()))
                }.isSuccess
            }
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())

            waitUntilOnMain {
                activity.supportFragmentManager.findFragmentByTag(AREA_TAG) == null
            }
        }
    }

    @Test
    fun eventsFailure_keepsContentVisible() {
        apiRule.server.dispatcher = areaDispatcher(
            eventsBody = """{"message":"boom"}""",
            eventsCode = 500,
        )

        withArea { _, area ->
            waitUntilOnMain {
                !area.requireView().findViewById<View>(R.id.loading).isVisible
            }

            Assert.assertTrue(
                area.requireView().findViewById<View>(R.id.content).isVisible,
            )
        }
    }

    @Test
    fun issuesFailure_keepsContentVisible() {
        apiRule.server.dispatcher = areaDispatcher(
            issuesBody = """{"message":"boom"}""",
            issuesCode = 500,
        )

        withArea { _, area ->
            waitUntilOnMain {
                !area.requireView().findViewById<View>(R.id.loading).isVisible
            }

            Assert.assertTrue(
                area.requireView().findViewById<View>(R.id.content).isVisible,
            )
        }
    }
}
