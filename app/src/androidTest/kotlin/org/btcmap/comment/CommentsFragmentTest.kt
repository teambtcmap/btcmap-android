package org.btcmap.comment

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.db.table.comment.Comment
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.hamcrest.Matchers.not
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class CommentsFragmentTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val apiRule = ApiRule()

    private val placeId = 1L

    private fun comment(id: Long, text: String, createdAt: String): Comment = Comment(
        id = id,
        placeId = placeId,
        comment = text,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(createdAt),
    )

    private fun launchComments(block: (ActivityScenario<Activity>, CommentsFragment) -> Unit) {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var fragment: CommentsFragment
            scenario.onActivity { activity ->
                fragment = CommentsFragment().apply {
                    arguments = Bundle().apply { putLong("place_id", placeId) }
                }
                activity.supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragmentContainerView, fragment, COMMENTS_TAG)
                }
                activity.supportFragmentManager.executePendingTransactions()
            }
            block(scenario, fragment)
        }
    }

    @Test
    fun showsStoredCommentsAndHidesTheEmptyState() {
        databaseRule.db.comment.insert(
            listOf(
                comment(1L, "First", "2024-06-01T10:00:00Z"),
                comment(2L, "Second", "2024-06-02T10:00:00Z"),
            )
        )

        launchComments { _, fragment ->
            waitUntilOnMain {
                fragment.requireView()
                    .findViewById<RecyclerView>(R.id.list)
                    .adapter?.itemCount == 2
            }
            onView(withText("Second")).check(matches(isDisplayed()))
            onView(withText("First")).check(matches(isDisplayed()))
            onView(withId(R.id.empty)).check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun showsEmptyStateWhenThereAreNoComments() {
        launchComments { _, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<View>(R.id.empty).isVisible
            }
            onView(withId(R.id.empty)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun fabOpensTheAddCommentScreen() {
        apiRule.server.dispatcher = quoteDispatcher()

        launchComments { scenario, _ ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            onView(withId(R.id.fab)).perform(click())

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AddCommentFragment
            }
        }
    }

    /**
     * End-to-end: open the add screen from the list, pay, and verify the freshly
     * posted comment is pulled in without leaving and re-entering the list.
     */
    @Test
    fun postedCommentAppearsInTheListAfterPayment() {
        val dispatcher = CommentsDispatcher()
        apiRule.server.dispatcher = dispatcher

        launchComments { scenario, fragment ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            waitUntilOnMain {
                fragment.requireView().findViewById<View>(R.id.empty).isVisible
            }
            // Let the first sync finish so the list is stable before posting.
            waitUntil { dispatcher.commentsRequests.get() >= 1 }

            onView(withId(R.id.fab)).perform(click())
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AddCommentFragment
            }
            waitUntilOnMain {
                activity.findViewById<Button>(R.id.btn_continue)?.isEnabled == true
            }

            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())

            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is CommentsFragment
            }

            waitUntil {
                try {
                    onView(withText("gm")).check(matches(isDisplayed()))
                    true
                } catch (t: Throwable) {
                    false
                }
            }
            onView(withId(R.id.empty)).check(matches(not(isDisplayed())))
        }
    }

    /**
     * The server can report the invoice as paid just before it publishes the
     * comment, so the list would sync once and still see the comment hidden.
     * The post-payment retry must pick it up without leaving the screen.
     */
    @Test
    fun postedCommentAppearsWhenTheServerPublishesItLate() {
        val dispatcher = DelayedPublishDispatcher()
        apiRule.server.dispatcher = dispatcher

        launchComments { scenario, fragment ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            waitUntilOnMain {
                fragment.requireView().findViewById<View>(R.id.empty).isVisible
            }
            waitUntil { dispatcher.commentsRequests.get() >= 1 }

            onView(withId(R.id.fab)).perform(click())
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AddCommentFragment
            }
            waitUntilOnMain {
                activity.findViewById<Button>(R.id.btn_continue)?.isEnabled == true
            }

            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())

            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is CommentsFragment
            }

            waitUntil {
                try {
                    onView(withText("gm")).check(matches(isDisplayed()))
                    true
                } catch (t: Throwable) {
                    false
                }
            }
            Assert.assertTrue(
                "the list must have retried the sync",
                dispatcher.commentsAfterPost.get() >= 2,
            )
            onView(withId(R.id.empty)).check(matches(not(isDisplayed())))
        }
    }

    /**
     * The old retry stopped as soon as any visible comment was stored. An
     * unrelated comment syncing in the meantime must not end the retries before
     * the paid comment is published.
     */
    @Test
    fun postedCommentAppearsEvenWhenAnotherCommentSyncsFirst() {
        val dispatcher = UnrelatedCommentDispatcher()
        apiRule.server.dispatcher = dispatcher

        launchComments { scenario, fragment ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            waitUntilOnMain {
                fragment.requireView().findViewById<View>(R.id.empty).isVisible
            }
            waitUntil { dispatcher.commentsRequests.get() >= 1 }

            onView(withId(R.id.fab)).perform(click())
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AddCommentFragment
            }
            waitUntilOnMain {
                activity.findViewById<Button>(R.id.btn_continue)?.isEnabled == true
            }

            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())

            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is CommentsFragment
            }

            waitUntil {
                try {
                    onView(withText("gm")).check(matches(isDisplayed()))
                    true
                } catch (t: Throwable) {
                    false
                }
            }
            Assert.assertTrue(
                "the list must have kept retrying past the unrelated comment",
                dispatcher.commentsAfterPost.get() >= 2,
            )
        }
    }

    /**
     * The empty state must stay hidden on the way back from a paid add screen
     * too, not only on the first load, or it flashes "no comments yet" while
     * the paid comment is still being published.
     */
    @Test
    fun emptyStateStaysHiddenWhileThePaidCommentIsPublished() {
        val dispatcher = NeverPublishDispatcher()
        apiRule.server.dispatcher = dispatcher

        launchComments { scenario, fragment ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            waitUntilOnMain {
                fragment.requireView().findViewById<View>(R.id.empty).isVisible
            }
            waitUntil { dispatcher.commentsRequests.get() >= 1 }

            onView(withId(R.id.fab)).perform(click())
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AddCommentFragment
            }
            waitUntilOnMain {
                activity.findViewById<Button>(R.id.btn_continue)?.isEnabled == true
            }

            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())

            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is CommentsFragment
            }

            // Let the retry run a couple of times. The comment is never
            // published, so the list is still genuinely empty; the empty state
            // must not be shown while the retry is still looking for it.
            waitUntil { dispatcher.commentsAfterPost.get() >= 2 }
            onView(withId(R.id.empty)).check(matches(not(isDisplayed())))
        }
    }

    private fun quoteDispatcher(): Dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            when (request.url.encodedPath) {
                "/v4/place-comments/quote" -> jsonResponse("""{"quote_sat":1000}""")
                else -> jsonResponse("[]")
            }
    }

    /**
     * Serves the quote and the order, records the requests, and reports the
     * invoice paid from the second poll on. Subclasses supply the
     * `/v4/place-comments` GET response through [commentsResponse].
     */
    private abstract class PaymentDispatcher : Dispatcher() {
        val posted = AtomicBoolean(false)
        val orderRequests = AtomicInteger()
        val commentsRequests = AtomicInteger()
        val invoiceRequests = AtomicInteger()
        val commentsAfterPost = AtomicInteger()

        protected abstract fun commentsResponse(): MockResponse

        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.url.encodedPath
            return when {
                path == "/v4/place-comments/quote" -> jsonResponse("""{"quote_sat":1000}""")

                path == "/v4/place-comments" && request.method == "POST" -> {
                    posted.set(true)
                    orderRequests.incrementAndGet()
                    jsonResponse("""{"invoice_id":"c1","invoice":"lnbc-c"}""")
                }

                path == "/v4/place-comments" -> {
                    commentsRequests.incrementAndGet()
                    commentsResponse()
                }

                path.startsWith("/v4/invoices/") -> {
                    val index = invoiceRequests.getAndIncrement()
                    val status = if (index == 0) "unpaid" else "paid"
                    jsonResponse("""{"id":"c1","status":"$status"}""")
                }

                else -> jsonResponse("[]")
            }
        }
    }

    /** Serves the posted comment as soon as the order exists. */
    private class CommentsDispatcher : PaymentDispatcher() {
        override fun commentsResponse(): MockResponse =
            if (posted.get()) jsonResponse(POSTED_COMMENT_JSON) else jsonResponse("[]")
    }

    /**
     * Like [CommentsDispatcher], but the first sync after the order still sees
     * the comment hidden, as happens when an invoice is reported as paid a
     * moment before the server publishes the comment.
     */
    private class DelayedPublishDispatcher : PaymentDispatcher() {
        override fun commentsResponse(): MockResponse = when {
            !posted.get() -> jsonResponse("[]")
            commentsAfterPost.getAndIncrement() == 0 -> jsonResponse(HIDDEN_COMMENT_JSON)
            else -> jsonResponse(POSTED_COMMENT_JSON)
        }
    }

    /**
     * Like [DelayedPublishDispatcher], but the first sync after the order also
     * brings a visible comment for another place. The old retry stopped on any
     * stored comment and would have left the paid comment hidden.
     */
    private class UnrelatedCommentDispatcher : PaymentDispatcher() {
        override fun commentsResponse(): MockResponse = when {
            !posted.get() -> jsonResponse("[]")
            commentsAfterPost.getAndIncrement() == 0 ->
                jsonResponse(HIDDEN_AND_UNRELATED_COMMENTS_JSON)
            else -> jsonResponse(POSTED_COMMENT_JSON)
        }
    }

    /** Serves the comments but never publishes the paid one. */
    private class NeverPublishDispatcher : PaymentDispatcher() {
        override fun commentsResponse(): MockResponse = when {
            !posted.get() -> jsonResponse("[]")
            else -> {
                commentsAfterPost.incrementAndGet()
                jsonResponse(HIDDEN_COMMENT_JSON)
            }
        }
    }

    private companion object {
        const val COMMENTS_TAG = "comments"

        const val POSTED_COMMENT_JSON =
            """[{"id":9,"place_id":1,"text":"gm","created_at":"2024-06-03T10:00:00Z","updated_at":"2024-06-03T10:00:00Z","deleted_at":null}]"""

        const val HIDDEN_COMMENT_JSON =
            """[{"id":9,"place_id":1,"text":"gm","created_at":"2024-06-03T10:00:00Z","updated_at":"2024-06-03T10:00:00Z","deleted_at":"2024-06-03T10:00:00Z"}]"""

        const val HIDDEN_AND_UNRELATED_COMMENTS_JSON =
            """[{"id":9,"place_id":1,"text":"gm","created_at":"2024-06-03T10:00:00Z","updated_at":"2024-06-03T10:00:00Z","deleted_at":"2024-06-03T10:00:00Z"},""" +
                """{"id":100,"place_id":999,"text":"other","created_at":"2024-06-03T10:00:00Z","updated_at":"2024-06-03T10:00:00Z","deleted_at":null}]"""

        fun jsonResponse(body: String, code: Int = 200): MockResponse =
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
    }
}
