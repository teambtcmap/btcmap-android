package org.btcmap.place

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
import org.btcmap.comment.AddCommentFragment
import org.btcmap.db.table.comment.Comment
import org.btcmap.db.table.place.Place
import org.btcmap.ui.PLACE_ADD_COMMENT_TAG
import org.btcmap.ui.PlaceComposeView
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntilOnMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class PlaceCommentsTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun storedComments_arePreviewedInline() = runBlocking<Unit> {
        databaseRule.db.comment.insert(
            listOf(
                comment(1L, "First", "2024-06-01T10:00:00Z"),
                comment(2L, "Second", "2024-06-02T10:00:00Z"),
            )
        )

        withPlaceFragment { _, _, _ ->
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithText("First").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("First").assertExists()
            composeTestRule.onNodeWithText("Second").assertExists()
        }
    }

    @Test
    fun addCommentButton_opensTheAddScreen() = runBlocking<Unit> {
        // A valid quote keeps the add screen open; otherwise it pops itself as
        // soon as the quote fails to load.
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                when (request.url.encodedPath) {
                    "/v4/place-comments/quote" -> jsonResponse("""{"quote_sat":1000}""")
                    else -> jsonResponse("[]")
                }
        }

        withPlaceFragment { _, activity, fragment ->
            waitUntilOnMain {
                fragment.requireView()
                    .findViewById<PlaceComposeView>(R.id.placeContent)
                    .place != null
            }
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithTag(PLACE_ADD_COMMENT_TAG)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithTag(PLACE_ADD_COMMENT_TAG).performClick()

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AddCommentFragment
            }
        }
    }

    private fun withPlaceFragment(
        action: (ActivityScenario<Activity>, Activity, PlaceFragment) -> Unit,
    ) {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var activity: Activity
                lateinit var fragment: PlaceFragment
                scenario.onActivity {
                    activity = it
                    fragment = PlaceFragment()
                    it.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, fragment, PLACE_TAG)
                    }
                    fragment.setPlace(placeRow())
                }
                action(scenario, activity, fragment)
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun comment(id: Long, text: String, createdAt: String): Comment = Comment(
        id = id,
        placeId = 1L,
        comment = text,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(createdAt),
    )

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
            comments = 2L,
            telegram = null,
            osmId = null,
        )
    }

    private companion object {
        const val PLACE_TAG = "place"
        const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"

        fun jsonResponse(body: String): MockResponse =
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
    }
}
