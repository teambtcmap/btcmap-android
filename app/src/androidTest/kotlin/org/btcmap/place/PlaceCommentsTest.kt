package org.btcmap.place

import android.widget.Button
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
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
import org.btcmap.comment.CommentsFragment
import org.btcmap.db.table.comment.Comment
import org.btcmap.db.table.place.Place
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntilOnMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class PlaceCommentsTest : AppTestCase() {

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun commentsPreview_showsStoredCommentsAndOpensTheList() {
        databaseRule.db.comment.insert(
            listOf(
                comment(1L, "First", "2024-06-01T10:00:00Z"),
                comment(2L, "Second", "2024-06-02T10:00:00Z"),
            )
        )

        withPlaceFragment { scenario, activity, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.comments).text ==
                    activity.getString(R.string.comments_d, 2)
            }
            waitUntilOnMain {
                fragment.requireView()
                    .findViewById<RecyclerView>(R.id.comments_list)
                    .adapter
                    ?.itemCount == 2
            }

            scenario.onActivity {
                fragment.requireView().findViewById<Button>(R.id.comments).performClick()
            }
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is CommentsFragment
            }
        }
    }

    @Test
    fun commentsButton_opensTheAddScreenWhenThereAreNoComments() {
        // A valid quote keeps the add screen open; otherwise it pops itself as
        // soon as the quote fails to load.
        apiRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                when (request.url.encodedPath) {
                    "/v4/place-comments/quote" -> jsonResponse("""{"quote_sat":1000}""")
                    else -> jsonResponse("[]")
                }
        }

        withPlaceFragment { scenario, activity, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.comments).isEnabled
            }

            scenario.onActivity {
                fragment.requireView().findViewById<Button>(R.id.comments).performClick()
            }
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
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(createdAt),
    )

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
            localizedOpeningHours = null,
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
