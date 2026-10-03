package org.btcmap.payment

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvoicePaymentFlowTest {

    private val invoice = PaymentInvoice(id = "inv-1", bolt11 = "lnbc1...")

    private fun TestScope.flow(
        quoteLoader: suspend () -> String = { "quote" },
        restoredInvoice: PaymentInvoice? = null,
        persist: (PaymentInvoice) -> Unit = {},
        clear: () -> Unit = {},
    ) = InvoicePaymentFlow(
        quoteLoader = quoteLoader,
        scope = this,
        restoredInvoice = restoredInvoice,
        persistInvoice = persist,
        clearPersistedInvoice = clear,
    )

    @Test
    fun initialState_waitsForQuoteButAllowsInput() = runTest {
        val flow = flow()

        Assert.assertTrue(flow.state.value.loadingQuote)
        Assert.assertFalse(flow.state.value.actionsEnabled)
        Assert.assertTrue(flow.state.value.inputEnabled)
    }

    @Test
    fun loadQuote_publishesQuoteAndEnablesActions() = runTest {
        val flow = flow()

        flow.loadQuote()
        advanceUntilIdle()

        Assert.assertEquals("quote", flow.state.value.quote)
        Assert.assertFalse(flow.state.value.loadingQuote)
        Assert.assertTrue(flow.state.value.actionsEnabled)
        Assert.assertTrue(flow.state.value.inputEnabled)
    }

    @Test
    fun loadQuote_onlyLoadsOnce() = runTest {
        var calls = 0
        val flow = flow(quoteLoader = { calls++; "quote" })

        flow.loadQuote()
        flow.loadQuote()
        advanceUntilIdle()

        Assert.assertEquals(1, calls)
    }

    @Test
    fun loadQuote_failureEmitsEventAndStopsLoading() = runTest {
        val error = IllegalStateException("boom")
        val flow = flow(quoteLoader = { throw error })

        flow.loadQuote()
        advanceUntilIdle()

        Assert.assertNull(flow.state.value.quote)
        Assert.assertFalse(flow.state.value.loadingQuote)
        Assert.assertFalse(flow.state.value.actionsEnabled)

        val event = flow.events.first()
        Assert.assertTrue(event is PaymentEvent.QuoteFailed)
        Assert.assertSame(error, event.error)
    }

    @Test
    fun loadQuote_canBeRetriedAfterFailure() = runTest {
        var calls = 0
        val flow = flow(
            quoteLoader = {
                calls++
                if (calls == 1) throw IllegalStateException("boom")
                "quote"
            },
        )

        flow.loadQuote()
        advanceUntilIdle()
        Assert.assertFalse(flow.state.value.loadingQuote)
        Assert.assertNull(flow.state.value.quote)

        flow.loadQuote()
        Assert.assertTrue(
            "a retry shows the loading state again",
            flow.state.value.loadingQuote,
        )
        advanceUntilIdle()

        Assert.assertEquals("quote", flow.state.value.quote)
        Assert.assertEquals(2, calls)
    }

    @Test
    fun order_publishesInvoiceAndLocksControls() = runTest {
        val flow = flow()
        flow.loadQuote()
        advanceUntilIdle()

        flow.order { invoice }
        advanceUntilIdle()

        Assert.assertEquals(invoice, flow.state.value.invoice)
        Assert.assertFalse(flow.state.value.ordering)
        Assert.assertFalse(flow.state.value.actionsEnabled)
        Assert.assertFalse(flow.state.value.inputEnabled)
    }

    @Test
    fun order_locksControlsWhileInFlight() = runTest {
        val gate = CompletableDeferred<Unit>()
        val flow = flow()
        flow.loadQuote()
        advanceUntilIdle()

        flow.order {
            gate.await()
            invoice
        }
        advanceUntilIdle()

        Assert.assertTrue(flow.state.value.ordering)
        Assert.assertFalse(flow.state.value.actionsEnabled)
        Assert.assertFalse(flow.state.value.inputEnabled)

        gate.complete(Unit)
        advanceUntilIdle()

        Assert.assertEquals(invoice, flow.state.value.invoice)
    }

    @Test
    fun order_isIgnoredOnceAnInvoiceExists() = runTest {
        val orders = mutableListOf<Int>()
        val flow = flow()
        flow.loadQuote()
        advanceUntilIdle()

        flow.order { orders.add(1); invoice }
        advanceUntilIdle()
        flow.order { orders.add(2); invoice }
        advanceUntilIdle()

        Assert.assertEquals(listOf(1), orders)
    }

    @Test
    fun order_isIgnoredBeforeTheQuoteLoads() = runTest {
        val orders = mutableListOf<Int>()
        val flow = flow()

        flow.order { orders.add(1); invoice }
        advanceUntilIdle()

        Assert.assertTrue("no order without a quote", orders.isEmpty())
        Assert.assertNull(flow.state.value.invoice)
    }

    @Test
    fun order_canBeRetriedAfterFailure() = runTest {
        var calls = 0
        val flow = flow()
        flow.loadQuote()
        advanceUntilIdle()

        flow.order {
            calls++
            if (calls == 1) throw IllegalStateException("boom")
            invoice
        }
        advanceUntilIdle()

        Assert.assertNull(flow.state.value.invoice)
        Assert.assertTrue(flow.state.value.actionsEnabled)
        Assert.assertTrue(flow.events.first() is PaymentEvent.OrderFailed)

        flow.order { calls++; invoice }
        advanceUntilIdle()

        Assert.assertEquals(invoice, flow.state.value.invoice)
        Assert.assertEquals(2, calls)
    }

    @Test
    fun order_persistsTheInvoice() = runTest {
        var persisted: PaymentInvoice? = null
        val flow = flow(persist = { persisted = it })
        flow.loadQuote()
        advanceUntilIdle()

        flow.order { invoice }
        advanceUntilIdle()

        Assert.assertEquals(invoice, persisted)
    }

    @Test
    fun restoredInvoice_isNotOrderedAgain() = runTest {
        val orders = mutableListOf<Int>()
        val flow = flow(restoredInvoice = invoice)
        flow.loadQuote()
        advanceUntilIdle()

        flow.order { orders.add(1); invoice }
        advanceUntilIdle()

        Assert.assertTrue("a restored invoice must not be ordered again", orders.isEmpty())
        Assert.assertEquals(invoice, flow.state.value.invoice)
        Assert.assertFalse(flow.state.value.actionsEnabled)
    }

    @Test
    fun startOver_clearsTheInvoiceAndAllowsReorder() = runTest {
        var cleared = false
        val flow = flow(clear = { cleared = true })
        flow.loadQuote()
        advanceUntilIdle()
        flow.order { invoice }
        advanceUntilIdle()

        flow.startOver()

        Assert.assertTrue(cleared)
        Assert.assertNull(flow.state.value.invoice)
        Assert.assertTrue(flow.state.value.actionsEnabled)
        Assert.assertTrue(flow.state.value.inputEnabled)

        flow.order { invoice }
        advanceUntilIdle()

        Assert.assertEquals(invoice, flow.state.value.invoice)
    }

    @Test
    fun startOver_doesNothingWithoutAnInvoice() = runTest {
        var cleared = false
        val flow = flow(clear = { cleared = true })
        flow.loadQuote()
        advanceUntilIdle()

        flow.startOver()

        Assert.assertFalse(cleared)
        Assert.assertTrue(flow.state.value.actionsEnabled)
    }

    @Test
    fun reportPaymentFailure_emitsEvent() = runTest {
        val error = IllegalStateException("payment polling failed")
        val flow = flow()

        flow.reportPaymentFailure(error)

        val event = flow.events.first()
        Assert.assertTrue(event is PaymentEvent.PaymentFailed)
        Assert.assertSame(error, event.error)
    }
}
