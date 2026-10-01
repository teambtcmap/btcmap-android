package org.btcmap.payment

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.fragment.app.Fragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.comment.AddCommentFragment
import org.btcmap.ui.AddCommentFormComposeView
import org.btcmap.ui.COMMENT_CONTINUE_TAG
import org.btcmap.ui.COMMENT_FIELD_TAG
import org.btcmap.ui.InvoicePaymentComposeView
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AddCommentPaymentFlowTest : PaymentScreenTest() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private fun invoiceShown(fragment: Fragment): Boolean =
        fragment.requireView().findViewById<InvoicePaymentComposeView>(R.id.invoicePayment).qr != null

    private fun actionsEnabled(fragment: Fragment): Boolean =
        fragment.requireView().findViewById<AddCommentFormComposeView>(R.id.addCommentForm)
            .state?.actionsEnabled == true

    private fun inputEnabled(fragment: Fragment): Boolean =
        fragment.requireView().findViewById<AddCommentFormComposeView>(R.id.addCommentForm)
            .state?.inputEnabled == true

    @Test
    fun quoteLoads_enablesContinueAndKeepsInputEditable() {
        apiRule.server.dispatcher = commentDispatcher()

        withComment { _, fragment ->
            waitUntilOnMain { actionsEnabled(fragment) && inputEnabled(fragment) }
        }
    }

    @Test
    fun emptyComment_doesNotOrder() {
        val dispatcher = commentDispatcher()
        apiRule.server.dispatcher = dispatcher

        withComment { _, fragment ->
            waitUntilOnMain { actionsEnabled(fragment) }
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()
            Thread.sleep(500)
            Assert.assertEquals(0, dispatcher.orderRequests.get())
        }
    }

    @Test
    fun continue_showsInvoiceAndLocksInput() {
        val dispatcher = commentDispatcher()
        apiRule.server.dispatcher = dispatcher

        withComment { _, fragment ->
            waitUntilOnMain { actionsEnabled(fragment) }
            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()

            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain {
                invoiceShown(fragment) && !inputEnabled(fragment) && !actionsEnabled(fragment)
            }
        }
    }

    @Test
    fun paidInvoice_closesScreen() {
        val dispatcher = commentDispatcher(invoiceStatuses = listOf("unpaid", "paid"))
        apiRule.server.dispatcher = dispatcher

        withComment(addToBackStack = true) { scenario, fragment ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            waitUntilOnMain { actionsEnabled(fragment) }
            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()

            waitUntilOnMain {
                activity.supportFragmentManager.findFragmentByTag(COMMENT_TAG) == null
            }
        }
    }

    /**
     * The place screen opens this screen directly and does not listen for the
     * posted result. Setting one anyway would leave a stale result behind for a
     * later comments visit to consume as a fresh payment.
     */
    @Test
    fun postedComment_setsNoResultWhenNotRequested() {
        val dispatcher = commentDispatcher(invoiceStatuses = listOf("unpaid", "paid"))
        apiRule.server.dispatcher = dispatcher

        withComment(addToBackStack = true) { scenario, fragment ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            val resultReceived = AtomicBoolean(false)
            scenario.onActivity {
                it.supportFragmentManager.setFragmentResultListener(
                    AddCommentFragment.REQUEST_KEY,
                    it,
                ) { _, _ -> resultReceived.set(true) }
            }

            waitUntilOnMain { actionsEnabled(fragment) }
            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()

            waitUntilOnMain {
                activity.supportFragmentManager.findFragmentByTag(COMMENT_TAG) == null
            }
            Assert.assertFalse(
                "an add screen opened directly must not set the retry result",
                resultReceived.get(),
            )
        }
    }

    @Test
    fun orderFailure_reenablesInputAndShowsDialog() {
        apiRule.server.dispatcher = commentDispatcher(
            orderBody = """{"message":"boom"}""",
            orderCode = 400,
        )

        withComment { _, fragment ->
            waitUntilOnMain { actionsEnabled(fragment) }
            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()

            waitUntil {
                try {
                    onView(withText("boom")).inRoot(isDialog())
                        .check(matches(isDisplayed()))
                    true
                } catch (t: Throwable) {
                    false
                }
            }
            waitUntilOnMain { inputEnabled(fragment) && actionsEnabled(fragment) }
            onView(withText(R.string.close)).inRoot(isDialog()).perform(click())
        }
    }

    @Test
    fun invoice_survivesRotation() {
        val dispatcher = commentDispatcher()
        apiRule.server.dispatcher = dispatcher

        withComment { scenario, fragment ->
            waitUntilOnMain { actionsEnabled(fragment) }
            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()
            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain { invoiceShown(fragment) }

            scenario.recreate()

            val recreated = restoredFragment(scenario, COMMENT_TAG) as AddCommentFragment
            waitUntilOnMain { invoiceShown(recreated) }
            Assert.assertEquals("quote must not be refetched", 1, dispatcher.quoteRequests.get())
            Assert.assertEquals("order must not be replaced", 1, dispatcher.orderRequests.get())
        }
    }
}
