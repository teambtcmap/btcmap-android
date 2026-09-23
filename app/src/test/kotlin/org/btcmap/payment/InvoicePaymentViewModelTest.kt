package org.btcmap.payment

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.btcmap.MainDispatcherRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvoicePaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invoice = PaymentInvoice(id = "inv-1", bolt11 = "lnbc1...")

    private fun viewModel(
        quoteLoader: suspend () -> String = { "quote" },
    ) = InvoicePaymentViewModel(quoteLoader)

    @Test
    fun initialState_waitsForQuoteButAllowsInput() = runTest(mainDispatcherRule.dispatcher) {
        val model = viewModel()

        Assert.assertTrue(model.state.value.loadingQuote)
        Assert.assertFalse(model.state.value.actionsEnabled)
        Assert.assertTrue(model.state.value.inputEnabled)
    }

    @Test
    fun factory_createsAViewModelThatLoadsTheQuote() = runTest(mainDispatcherRule.dispatcher) {
        val model = InvoicePaymentViewModel.Factory { "quote" }
            .create(InvoicePaymentViewModel::class.java)

        model.loadQuote()
        advanceUntilIdle()

        Assert.assertEquals("quote", model.state.value.quote)
        Assert.assertTrue(model.state.value.actionsEnabled)
    }

    @Test
    fun factory_rejectsAnUnrelatedViewModelClass() {
        val factory = InvoicePaymentViewModel.Factory { "quote" }

        Assert.assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnrelatedViewModel::class.java)
        }
    }

    @Test
    fun loadQuote_publishesQuoteAndEnablesActions() = runTest(mainDispatcherRule.dispatcher) {
        val model = viewModel()

        model.loadQuote()
        advanceUntilIdle()

        Assert.assertEquals("quote", model.state.value.quote)
        Assert.assertFalse(model.state.value.loadingQuote)
        Assert.assertTrue(model.state.value.actionsEnabled)
        Assert.assertTrue(model.state.value.inputEnabled)
    }

    @Test
    fun loadQuote_onlyLoadsOnce() = runTest(mainDispatcherRule.dispatcher) {
        var calls = 0
        val model = viewModel(quoteLoader = { calls++; "quote" })

        model.loadQuote()
        model.loadQuote()
        advanceUntilIdle()

        Assert.assertEquals(1, calls)
    }

    @Test
    fun loadQuote_failureEmitsEventAndStopsLoading() = runTest(mainDispatcherRule.dispatcher) {
        val error = IllegalStateException("boom")
        val model = viewModel(quoteLoader = { throw error })

        model.loadQuote()
        advanceUntilIdle()

        Assert.assertNull(model.state.value.quote)
        Assert.assertFalse(model.state.value.loadingQuote)
        Assert.assertFalse(model.state.value.actionsEnabled)

        val event = model.events.first()
        Assert.assertTrue(event is PaymentEvent.QuoteFailed)
        Assert.assertSame(error, (event as PaymentEvent.QuoteFailed).error)
    }

    @Test
    fun order_publishesInvoiceAndLocksControls() = runTest(mainDispatcherRule.dispatcher) {
        val model = viewModel()
        model.loadQuote()
        advanceUntilIdle()

        model.order { invoice }
        advanceUntilIdle()

        Assert.assertEquals(invoice, model.state.value.invoice)
        Assert.assertFalse(model.state.value.ordering)
        Assert.assertFalse(model.state.value.actionsEnabled)
        Assert.assertFalse(model.state.value.inputEnabled)
    }

    @Test
    fun order_locksControlsWhileInFlight() = runTest(mainDispatcherRule.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val model = viewModel()
        model.loadQuote()
        advanceUntilIdle()

        model.order {
            gate.await()
            invoice
        }
        advanceUntilIdle()

        Assert.assertTrue(model.state.value.ordering)
        Assert.assertFalse(model.state.value.actionsEnabled)
        Assert.assertFalse(model.state.value.inputEnabled)

        gate.complete(Unit)
        advanceUntilIdle()

        Assert.assertEquals(invoice, model.state.value.invoice)
    }

    @Test
    fun order_isIgnoredOnceAnInvoiceExists() = runTest(mainDispatcherRule.dispatcher) {
        val orders = mutableListOf<Int>()
        val model = viewModel()
        model.loadQuote()
        advanceUntilIdle()

        model.order { orders.add(1); invoice }
        advanceUntilIdle()
        model.order { orders.add(2); invoice }
        advanceUntilIdle()

        Assert.assertEquals(listOf(1), orders)
        Assert.assertEquals(invoice, model.state.value.invoice)
    }

    @Test
    fun order_canBeRetriedAfterFailure() = runTest(mainDispatcherRule.dispatcher) {
        var calls = 0
        val model = viewModel()
        model.loadQuote()
        advanceUntilIdle()

        model.order {
            calls++
            if (calls == 1) throw IllegalStateException("boom")
            invoice
        }
        advanceUntilIdle()

        Assert.assertNull(model.state.value.invoice)
        Assert.assertTrue(model.state.value.actionsEnabled)
        Assert.assertTrue(model.events.first() is PaymentEvent.OrderFailed)

        model.order { calls++; invoice }
        advanceUntilIdle()

        Assert.assertEquals(invoice, model.state.value.invoice)
        Assert.assertEquals(2, calls)
    }

    @Test
    fun reportPaymentFailure_emitsEvent() = runTest(mainDispatcherRule.dispatcher) {
        val error = IllegalStateException("payment polling failed")
        val model = viewModel()

        model.reportPaymentFailure(error)

        val event = model.events.first()
        Assert.assertTrue(event is PaymentEvent.PaymentFailed)
        Assert.assertSame(error, (event as PaymentEvent.PaymentFailed).error)
    }
}

private class UnrelatedViewModel : ViewModel()
