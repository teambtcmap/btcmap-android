package org.btcmap.area

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class AreaLoadErrorHandlingTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun loadSuccess_hidesLoadingIndicatorAndShowsContent() {
        val release = CountDownLatch(1)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.url.encodedPath.startsWith("/v4/areas")) {
                    release.await()
                }
                return jsonResponse(AREA_JSON)
            }
        }

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var area: AreaFragment
                scenario.onActivity { activity ->
                    area = AreaFragment().apply {
                        arguments = Bundle().apply { putString("area_id", "1") }
                    }
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, area, AREA_TAG)
                    }
                }
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
        } finally {
            release.countDown()
            app.mapStyleUriForTesting = null
        }
    }

    @Test
    fun loadFailure_showsErrorDialogWithDetails_andClosesScreenOnDismiss() {
        val release = CountDownLatch(1)
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val isArea = request.url.encodedPath.startsWith("/v4/areas")
                if (isArea) {
                    release.await()
                }
                return MockResponse.Builder()
                    .code(if (isArea) 500 else 200)
                    .addHeader("Content-Type", "application/json")
                    .body(if (isArea) """{"message":"boom"}""" else "[]")
                    .build()
            }
        }

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var activity: Activity
                lateinit var area: AreaFragment
                scenario.onActivity {
                    activity = it
                    area = AreaFragment().apply {
                        arguments = Bundle().apply { putString("area_id", "1") }
                    }
                    activity.supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, area, AREA_TAG)
                        addToBackStack(null)
                    }
                }
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
        } finally {
            release.countDown()
            app.mapStyleUriForTesting = null
        }
    }

    companion object {
        private const val AREA_TAG = "area"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"

        private const val AREA_JSON = """
            {
                "id": 1,
                "name": "Grand Paris",
                "type": "community",
                "url_alias": "grand-paris",
                "icon": null,
                "icon_wide": null,
                "website_url": "https://btcmap.org/community/grand-paris",
                "description": null
            }
        """

        private fun jsonResponse(body: String): MockResponse {
            return MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
        }
    }
}
