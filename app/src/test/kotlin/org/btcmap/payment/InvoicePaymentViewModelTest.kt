package org.btcmap.payment

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.btcmap.MainDispatcherRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/**
 * The Android glue around the shared [InvoicePaymentFlow]: the pending invoice
 * must survive process death through the fragment's [SavedStateHandle]. The
 * state machine itself is covered by `InvoicePaymentFlowTest` in `:shared`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InvoicePaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invoice = PaymentInvoice(id = "inv-1", bolt11 = "lnbc1...")

    private fun viewModel(savedStateHandle: SavedStateHandle) =
        InvoicePaymentViewModel({ "quote" }, savedStateHandle)

    @Test
    fun order_persistsTheInvoiceForProcessDeath() = runTest(mainDispatcherRule.dispatcher) {
        val savedStateHandle = SavedStateHandle()
        val model = viewModel(savedStateHandle)
        model.loadQuote()
        advanceUntilIdle()

        model.order { invoice }
        advanceUntilIdle()

        // A new view model stands in for the process being recreated.
        val restored = viewModel(savedStateHandle)
        restored.loadQuote()
        advanceUntilIdle()

        Assert.assertEquals(invoice, restored.state.value.invoice)
        Assert.assertFalse(restored.state.value.actionsEnabled)
    }

    @Test
    fun restoredInvoice_isNotOrderedAgain() = runTest(mainDispatcherRule.dispatcher) {
        val savedStateHandle = SavedStateHandle()
        val model = viewModel(savedStateHandle)
        model.loadQuote()
        advanceUntilIdle()
        model.order { invoice }
        advanceUntilIdle()

        val orders = mutableListOf<Int>()
        val restored = viewModel(savedStateHandle)
        restored.loadQuote()
        advanceUntilIdle()

        restored.order { orders.add(1); invoice }
        advanceUntilIdle()

        Assert.assertTrue("a restored invoice must not be ordered again", orders.isEmpty())
    }

    @Test
    fun startOver_clearsThePersistedInvoice() = runTest(mainDispatcherRule.dispatcher) {
        val savedStateHandle = SavedStateHandle()
        val model = viewModel(savedStateHandle)
        model.loadQuote()
        advanceUntilIdle()
        model.order { invoice }
        advanceUntilIdle()

        model.startOver()

        // The discarded invoice must not come back after a process restart.
        Assert.assertNull(viewModel(savedStateHandle).state.value.invoice)
    }
}
