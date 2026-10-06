package org.btcmap.feed

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
import org.btcmap.db.table.place.Place
import org.btcmap.nav.AppRootFragment
import org.btcmap.ui.AppRoute
import org.btcmap.ui.PLACE_MENU_TAG
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class ActivityFeedPlaceNavigationTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun tappingRow_opensThePlaceScreen() = runBlocking<Unit> {
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.url.encodedPath == "/v4/activity") {
                    jsonResponse(
                        """[{"type":"place_added","place_id":1,""" +
                            """"place_name":"Test Merchant","date":"2024-01-01T00:00:00Z"}]"""
                    )
                } else {
                    jsonResponse("[]")
                }
            }
        }
        databaseRule.db.place.insert(listOf(placeRow()))

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
                composeTestRule.waitUntil(5_000) {
                    composeTestRule.onAllNodesWithText("Test Merchant")
                        .fetchSemanticsNodes().isNotEmpty()
                }

                composeTestRule.onNodeWithText("Test Merchant").performClick()

                // The row opens the place screen, which pushes onto the same root.
                composeTestRule.waitUntil(5_000) {
                    composeTestRule.onAllNodesWithTag(PLACE_MENU_TAG)
                        .fetchSemanticsNodes().isNotEmpty()
                }
                composeTestRule.onNodeWithText("Test Merchant").assertExists()
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun placeRow(): Place {
        return Place(
            id = 1,
            updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
            lat = 0.0,
            lon = 0.0,
            icon = "storefront",
            name = "Test Merchant",
            localizedName = null,
            verifiedAt = null,
            address = null,
            openingHours = null,
            phone = null,
            website = null,
            email = null,
            twitter = null,
            facebook = null,
            instagram = null,
            line = null,
            requiredAppUrl = null,
            boostedUntil = null,
            comments = null,
            telegram = null,
            osmId = null,
        )
    }

    private companion object {
        val FEED_ROUTE = AppRoute.Feed(
            areaIds = listOf("1"),
            areaNames = listOf("Area"),
            areaTypes = listOf("community"),
        )
        const val FEED_TAG = "feed"
        const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"

        fun jsonResponse(body: String): MockResponse =
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
    }
}
