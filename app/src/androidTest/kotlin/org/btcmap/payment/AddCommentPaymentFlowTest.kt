package org.btcmap.payment

import org.btcmap.i18n.Strings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.R
import org.btcmap.ui.COMMENT_CONTINUE_TAG
import org.btcmap.ui.COMMENT_FIELD_TAG
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The add-comment screen now renders the shared
 * [org.btcmap.ui.CommentScreen], so these drive the form and the invoice section
 * through the shared UI.
 */
@RunWith(AndroidJUnit4::class)
class AddCommentPaymentFlowTest : PaymentScreenTest() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val startOver = Strings.current()["start_over"]

    private fun continueShown(): Boolean =
        composeTestRule.onAllNodesWithTag(COMMENT_CONTINUE_TAG).fetchSemanticsNodes().isNotEmpty()

    private fun fieldShown(): Boolean =
        composeTestRule.onAllNodesWithTag(COMMENT_FIELD_TAG).fetchSemanticsNodes().isNotEmpty()

    private fun invoiceShown(): Boolean =
        composeTestRule.onAllNodesWithText(startOver).fetchSemanticsNodes().isNotEmpty()

    private fun showsText(text: String): Boolean =
        composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun quoteLoads_enablesContinueAndKeepsInputEditable() {
        apiRule.server.dispatcher = commentDispatcher()

        withComment { _, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).assertIsEnabled()
            Assert.assertTrue(fieldShown())
        }
    }

    @Test
    fun emptyComment_doesNotOrder() {
        val dispatcher = commentDispatcher()
        apiRule.server.dispatcher = dispatcher

        withComment { _, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()
            Thread.sleep(500)
            Assert.assertEquals(0, dispatcher.orderRequests.get())
        }
    }

    @Test
    fun continue_showsInvoiceAndLocksInput() {
        val dispatcher = commentDispatcher()
        apiRule.server.dispatcher = dispatcher

        withComment { _, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }
            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()

            waitUntil { dispatcher.orderRequests.get() == 1 }
            composeTestRule.waitUntil(5_000) { invoiceShown() && !continueShown() && !fieldShown() }
        }
    }

    @Test
    fun paidInvoice_closesScreen() {
        apiRule.server.dispatcher = commentDispatcher(invoiceStatuses = listOf("unpaid", "paid"))

        withComment(addToBackStack = true) { scenario, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }
            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()

            waitUntil {
                var gone = false
                scenario.onActivity {
                    gone = it.supportFragmentManager.findFragmentByTag(COMMENT_TAG) == null
                }
                gone
            }
        }
    }

    @Test
    fun orderFailure_reenablesInputAndShowsError() {
        apiRule.server.dispatcher = commentDispatcher(
            orderBody = """{"message":"boom"}""",
            orderCode = 400,
        )

        withComment { _, _ ->
            composeTestRule.waitUntil(5_000) { continueShown() }
            composeTestRule.onNodeWithTag(COMMENT_FIELD_TAG).performTextInput("gm")
            composeTestRule.onNodeWithTag(COMMENT_CONTINUE_TAG).performClick()

            composeTestRule.waitUntil(5_000) { showsText("boom") }
            composeTestRule.waitUntil(5_000) { continueShown() }
        }
    }
}
