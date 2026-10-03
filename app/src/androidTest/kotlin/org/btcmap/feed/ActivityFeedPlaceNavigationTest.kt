package org.btcmap.feed

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.test.junit4.createEmptyComposeRule
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
import org.btcmap.place.PlaceFragment
import org.btcmap.ui.PlaceComposeView
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class ActivityFeedPlaceNavigationTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun tappingRow_opensThePlaceWithItsPreviewMap() {
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
                lateinit var activity: Activity
                lateinit var feed: TestActivityFeedTab
                scenario.onActivity {
                    activity = it
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
                composeTestRule.waitUntil(5_000) {
                    composeTestRule.onAllNodesWithText("Test Merchant")
                        .fetchSemanticsNodes().isNotEmpty()
                }

                composeTestRule.onNodeWithText("Test Merchant").performClick()

                waitUntilOnMain {
                    val current = activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView)
                    current is PlaceFragment &&
                        current.requireView().findViewById<Toolbar>(R.id.toolbar)
                            .title == "Test Merchant"
                }

                scenario.onActivity {
                    val place = activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView) as PlaceFragment

                    Assert.assertEquals(
                        "The tapped row's place id must reach the place screen",
                        1L,
                        place.requireArguments().getLong(PlaceFragment.ARG_PLACE_ID),
                    )
                    Assert.assertEquals(
                        "A place opened outside the map fills the shared body",
                        1L,
                        place.requireView()
                            .findViewById<PlaceComposeView>(R.id.placeContent)
                            .place
                            ?.id,
                    )
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun placeRow(): Place {
        return Place(
            id = 1,
            updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
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
