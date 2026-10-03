package org.btcmap.comment

import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.db.table.comment.Comment
import org.btcmap.ui.COMMENT_CONTINUE_TAG
import org.btcmap.ui.COMMENT_FIELD_TAG
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class CommentsFragmentTest : AppTestCase() {

    private val placeId = 1L

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val addDescription =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.add)

    private val noComments =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.no_comments_yet)

    private fun comment(id: Long, text: String, createdAt: String): Comment = Comment(
        id = id,
        placeId = placeId,
        comment = text,
        createdAt = ZonedDateTime.parse(createdAt),
        updatedAt = ZonedDateTime.parse(createdAt),
    )

    private fun showsText(text: String): Boolean =
        composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun launchComments(block: (ActivityScenario<Activity>) -> Unit) {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(
                        R.id.fragmentContainerView,
                        CommentsFragment().apply {
                            arguments = Bundle().apply { putLong("place_id", placeId) }
                        },
                        COMMENTS_TAG,
                    )
                }
                activity.supportFragmentManager.executePendingTransactions()
            }
            block(scenario)
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

        launchComments {
            composeTestRule.waitUntil(5_000) { showsText("Second") }
            composeTestRule.onNodeWithText("Second").assertIsDisplayed()
            composeTestRule.onNodeWithText("First").assertIsDisplayed()
            Assert.assertFalse(showsText(noComments))
        }
    }

    @Test
    fun showsEmptyStateWhenThereAreNoComments() {
        launchComments {
            composeTestRule.waitUntil(5_000) { showsText(noComments) }
            composeTestRule.onNodeWithText(noComments).assertIsDisplayed()
        }
    }

    @Test
    fun fabOpensTheAddCommentScreen() {
        apiRule.server.dispatcher = quoteDispatcher()

        launchComments { scenario ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            composeTestRule.onNodeWithContentDescription(addDescription).performClick()

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AddCommentFragment
            }
        }
    }

    /**
     * End-to-end: open the add screen from the list, pay, and the freshly posted
     * comment is pulled in without leaving and re-entering the list.
     */
    @Test
    fun postedCommentAppearsInTheListAfterPayment() {
        apiRule.server.dispatcher = CommentsDispatcher()

        launchComments { scenario ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            composeTestRule.waitUntil(5_000) { showsText(noComments) }

            composeTestRule.onNodeWithContentDescription(addDescription).performClick()
            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is AddCommentFragment
            }
            waitUntilOnMain {
                runCatching {
                    composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).assertIsEnabled()
                }.isSuccess
            }

            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()

            waitUntilOnMain {
                activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) is CommentsFragment
            }

            composeTestRule.waitUntil(5_000) { showsText("gm") }
            composeTestRule.onNodeWithText("gm").assertIsDisplayed()
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
     * Serves the quote and the order, reports the invoice paid from the second
     * poll on, and publishes the posted comment once the order exists.
     */
    private class CommentsDispatcher : Dispatcher() {
        private val posted = AtomicBoolean(false)
        private val invoiceRequests = AtomicInteger()

        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.url.encodedPath
            return when {
                path == "/v4/place-comments/quote" -> jsonResponse("""{"quote_sat":1000}""")

                path == "/v4/place-comments" && request.method == "POST" -> {
                    posted.set(true)
                    jsonResponse("""{"invoice_id":"c1","invoice":"lnbc-c"}""")
                }

                path == "/v4/place-comments" ->
                    if (posted.get()) jsonResponse(POSTED_COMMENT_JSON) else jsonResponse("[]")

                path.startsWith("/v4/invoices/") -> {
                    val index = invoiceRequests.getAndIncrement()
                    val status = if (index == 0) "unpaid" else "paid"
                    jsonResponse("""{"id":"c1","status":"$status"}""")
                }

                else -> jsonResponse("[]")
            }
        }
    }

    private companion object {
        const val COMMENTS_TAG = "comments"

        const val POSTED_COMMENT_JSON =
            """[{"id":9,"place_id":1,"text":"gm","created_at":"2024-06-03T10:00:00Z","updated_at":"2024-06-03T10:00:00Z","deleted_at":null}]"""

        fun jsonResponse(body: String, code: Int = 200): MockResponse =
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
    }
}
