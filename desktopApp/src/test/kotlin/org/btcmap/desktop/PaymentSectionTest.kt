package org.btcmap.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.btcmap.ui.AppTheme

/**
 * The shared invoice block with the desktop's actions. The QR is generated
 * locally from the invoice, so no network takes part.
 */
class PaymentSectionTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun invoice_showsThePayCopyAndStartOverActions() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    InvoicePaymentSection(invoice = Invoice("id", BOLT11), onStartOver = {})
                }
            }
            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithText("Copy").assertIsDisplayed()
            onNodeWithText("Start over").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun startOver_asksBeforeDiscarding() {
        var discarded = false
        runComposeUiTest {
            setContent {
                AppTheme {
                    InvoicePaymentSection(
                        invoice = Invoice("id", BOLT11),
                        onStartOver = { discarded = true },
                    )
                }
            }
            onNodeWithText("Start over").performClick()
            // The invoice is not dropped until the confirmation is accepted.
            assertFalse(discarded)
            onNodeWithTag(PAYMENT_DISCARD_TAG).performClick()
        }
        assertTrue(discarded)
    }

    private companion object {
        const val BOLT11 = "lnbc1u1p3exampleinvoice"
    }
}
