package org.btcmap.payment

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.R
import org.btcmap.ui.BOOST_CONTINUE_TAG
import org.btcmap.ui.BOOST_OPTION_TAG_PREFIX
import org.btcmap.ui.PAYMENT_DISCARD_TAG
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The boost screen now renders the shared [org.btcmap.ui.BoostScreen], so these
 * drive the form and the invoice section through the shared UI rather than the
 * old Android host state.
 */
@RunWith(AndroidJUnit4::class)
class BoostPaymentFlowTest : PaymentScreenTest() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val startOver =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.start_over)

    private fun continueShown(): Boolean =
        composeTestRule.onAllNodesWithTag(BOOST_CONTINUE_TAG).fetchSemanticsNodes().isNotEmpty()

    private fun invoiceShown(): Boolean =
        composeTestRule.onAllNodesWithText(startOver).fetchSemanticsNodes().isNotEmpty()

    private fun clickContinue() {
        composeTestRule.onNodeWithTag(BOOST_CONTINUE_TAG).performClick()
    }

    @Test
    fun quoteLoads_enablesContinue() {
        apiRule.server.dispatcher = boostDispatcher()

        withBoost { _, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }
            composeTestRule.onNodeWithTag(BOOST_CONTINUE_TAG).assertIsEnabled()
        }
    }

    @Test
    fun continue_showsInvoiceAndBlocksASecondOrder() {
        val dispatcher = boostDispatcher()
        apiRule.server.dispatcher = dispatcher

        withBoost { _, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }

            clickContinue()
            waitUntil { dispatcher.orderRequests.get() == 1 }
            // The invoice block replaces the order controls.
            composeTestRule.waitUntil(5_000) { invoiceShown() && !continueShown() }

            Assert.assertEquals(1, dispatcher.orderRequests.get())
        }
    }

    @Test
    fun continue_sendsTheSelectedDuration() {
        val dispatcher = boostDispatcher()
        apiRule.server.dispatcher = dispatcher

        withBoost { _, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }

            composeTestRule
                .onNodeWithTag(BOOST_OPTION_TAG_PREFIX + "TWELVE_MONTHS")
                .performClick()
            clickContinue()
            waitUntil { dispatcher.orderBodies.isNotEmpty() }

            Assert.assertEquals(
                """{"place_id":"1","days":365}""",
                dispatcher.orderBodies.first(),
            )
        }
    }

    @Test
    fun paidInvoice_closesScreen() {
        apiRule.server.dispatcher = boostDispatcher(invoiceStatuses = listOf("unpaid", "paid"))

        withBoost(addToBackStack = true) { scenario, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }
            clickContinue()

            waitUntil {
                var gone = false
                scenario.onActivity {
                    gone = it.supportFragmentManager.findFragmentByTag(BOOST_TAG) == null
                }
                gone
            }
        }
    }

    @Test
    fun startOver_discardsTheInvoiceAndAllowsAnotherOrder() {
        val dispatcher = boostDispatcher()
        apiRule.server.dispatcher = dispatcher

        withBoost { _, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }

            clickContinue()
            waitUntil { dispatcher.orderRequests.get() == 1 }
            composeTestRule.waitUntil(5_000) { invoiceShown() }

            composeTestRule.onNodeWithText(startOver).performClick()
            composeTestRule.onNodeWithTag(PAYMENT_DISCARD_TAG).performClick()

            composeTestRule.waitUntil(5_000) { !invoiceShown() && continueShown() }
            Assert.assertEquals(
                "starting over must not place an order by itself",
                1,
                dispatcher.orderRequests.get(),
            )

            clickContinue()
            waitUntil { dispatcher.orderRequests.get() == 2 }
        }
    }
}
