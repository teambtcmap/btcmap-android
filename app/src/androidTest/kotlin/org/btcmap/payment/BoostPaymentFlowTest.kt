package org.btcmap.payment

import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.boost.BoostFragment
import org.btcmap.util.waitUntil
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoostPaymentFlowTest : PaymentScreenTest() {

    @Test
    fun quoteLoads_enablesContinue() {
        apiRule.server.dispatcher = boostDispatcher()

        withBoost { _, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
        }
    }

    @Test
    fun continue_showsInvoiceAndBlocksASecondOrder() {
        val dispatcher = boostDispatcher()
        apiRule.server.dispatcher = dispatcher

        withBoost { scenario, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }

            onView(withId(R.id.btn_continue)).perform(click())
            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain {
                fragment.requireView().findViewById<View>(R.id.qr).isVisible &&
                    !fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }

            // Even a stray tap on the now-disabled continue must not order again.
            scenario.onActivity {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).performClick()
            }
            Thread.sleep(300)
            Assert.assertEquals(1, dispatcher.orderRequests.get())
        }
    }

    @Test
    fun invoice_survivesRotation() {
        val dispatcher = boostDispatcher()
        apiRule.server.dispatcher = dispatcher

        withBoost { scenario, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withId(R.id.btn_continue)).perform(click())
            waitUntil { dispatcher.orderRequests.get() == 1 }
            waitUntilOnMain { fragment.requireView().findViewById<View>(R.id.qr).isVisible }

            scenario.recreate()

            val recreated = restoredFragment(scenario, BOOST_TAG) as BoostFragment
            waitUntilOnMain { recreated.requireView().findViewById<View>(R.id.qr).isVisible }
            Assert.assertEquals("quote must not be refetched", 1, dispatcher.quoteRequests.get())
            Assert.assertEquals("order must not be replaced", 1, dispatcher.orderRequests.get())
        }
    }

    @Test
    fun paidInvoice_closesScreen() {
        val dispatcher = boostDispatcher(invoiceStatuses = listOf("unpaid", "paid"))
        apiRule.server.dispatcher = dispatcher

        withBoost(addToBackStack = true) { scenario, fragment ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withId(R.id.btn_continue)).perform(click())

            waitUntilOnMain {
                activity.supportFragmentManager.findFragmentByTag(BOOST_TAG) == null
            }
        }
    }

    @Test
    fun orderFailure_showsDialogAndReenablesControls() {
        apiRule.server.dispatcher = boostDispatcher(
            orderBody = """{"message":"boom"}""",
            orderCode = 400,
        )

        withBoost { _, fragment ->
            waitUntilOnMain {
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
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
                fragment.requireView().findViewById<Button>(R.id.btn_continue).isEnabled
            }
            onView(withText(R.string.close)).inRoot(isDialog()).perform(click())
        }
    }

    @Test
    fun quoteFailure_closesScreenAndShowsDialog() {
        apiRule.server.dispatcher = boostDispatcher(
            quoteBody = """{"message":"boom"}""",
            quoteCode = 400,
        )

        withBoost(addToBackStack = true) { scenario, _ ->
            lateinit var activity: Activity
            scenario.onActivity { activity = it }

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
                activity.supportFragmentManager.findFragmentByTag(BOOST_TAG) == null
            }
            onView(withText(R.string.close)).inRoot(isDialog()).perform(click())
        }
    }
}
