package org.btcmap.payment

import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.comment.AddCommentFragment
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AddCommentPaymentFlowTest : PaymentScreenTest() {

    @Test
    fun quoteLoads_enablesContinueAndKeepsInputEditable() {
        apiRule.server.dispatcher = commentDispatcher()

        withComment { _, fragment ->
            waitUntilOnMain {
                val view = fragment.requireView()
                view.findViewById<Button>(R.id.btn_continue).isEnabled &&
                    view.findViewById<EditText>(R.id.comment).isEnabled
            }
        }
    }

    @Test
    fun emptyComment_doesNotOrder() {
        val dispatcher = commentDispatcher()
        apiRule.server.dispatcher = dispatcher

        withComment { _, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withId(R.id.btn_continue)).perform(click())
            Thread.sleep(500)
            Assert.assertEquals(0, dispatcher.orderRequests.get())
        }
    }

    @Test
    fun continue_showsInvoiceAndLocksInput() {
        val dispatcher = commentDispatcher()
        apiRule.server.dispatcher = dispatcher

        withComment { _, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())

            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain {
                val view = fragment.requireView()
                view.findViewById<View>(R.id.qr).isVisible &&
                    !view.findViewById<EditText>(R.id.comment).isEnabled &&
                    !view.findViewById<Button>(R.id.btn_continue).isEnabled
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

            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())

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

            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())

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
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())

            waitUntil {
                try {
                    onView(withText("boom")).inRoot(isDialog())
                        .check(matches(isDisplayed()))
                    true
                } catch (t: Throwable) {
                    false
                }
            }
            waitUntilOnMain {
                val view = fragment.requireView()
                view.findViewById<EditText>(R.id.comment).isEnabled &&
                    view.findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withText(R.string.close)).inRoot(isDialog()).perform(click())
        }
    }

    @Test
    fun invoice_survivesRotation() {
        val dispatcher = commentDispatcher()
        apiRule.server.dispatcher = dispatcher

        withComment { scenario, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withId(R.id.comment)).perform(typeText("gm"), closeSoftKeyboard())
            onView(withId(R.id.btn_continue)).perform(click())
            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain { fragment.requireView().findViewById<View>(R.id.qr).isVisible }

            scenario.recreate()

            val recreated = restoredFragment(scenario, COMMENT_TAG) as AddCommentFragment
            waitUntilOnMain { recreated.requireView().findViewById<View>(R.id.qr).isVisible }
            Assert.assertEquals("quote must not be refetched", 1, dispatcher.quoteRequests.get())
            Assert.assertEquals("order must not be replaced", 1, dispatcher.orderRequests.get())
        }
    }
}
