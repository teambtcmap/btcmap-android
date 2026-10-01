package org.btcmap.payment

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvoicePaymentPollerTest {

    @Test
    fun changingTheInvoiceCancelsThePreviousPoll() = runTest {
        val ids = MutableStateFlow<String?>(null)
        val polls = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        val poller = InvoicePaymentPoller { id ->
            polls += id
            try {
                awaitCancellation()
            } finally {
                cancelled += id
            }
        }

        val job = launch {
            poller.awaitPayment(
                ids = ids,
                onPaid = { throw AssertionError("no invoice should be paid") },
                onFailure = { throw AssertionError("no invoice should fail") },
            )
        }

        ids.value = "inv-1"
        runCurrent()
        Assert.assertEquals(listOf("inv-1"), polls)

        // Starting over clears the invoice, so the in-flight poll must stop
        // instead of outliving the state that started it and keeping the next
        // invoice from ever being watched.
        ids.value = null
        runCurrent()
        Assert.assertEquals(listOf("inv-1"), cancelled)

        ids.value = "inv-2"
        runCurrent()
        Assert.assertEquals(listOf("inv-1", "inv-2"), polls)

        job.cancelAndJoin()
    }

    @Test
    fun paidInvoiceIsReportedOncePerPoller() = runTest {
        val ids = MutableStateFlow<String?>(null)
        val polls = mutableListOf<String>()
        var paid = 0
        val poller = InvoicePaymentPoller { id -> polls += id }

        val job = launch {
            poller.awaitPayment(ids, onPaid = { paid++ }, onFailure = {})
        }

        ids.value = "inv-1"
        runCurrent()
        Assert.assertEquals(1, paid)

        // Re-emitting the same invoice, as a resumed view does, must not poll
        // or report it again.
        ids.value = null
        runCurrent()
        ids.value = "inv-1"
        runCurrent()

        Assert.assertEquals(listOf("inv-1"), polls)
        Assert.assertEquals(1, paid)

        job.cancelAndJoin()
    }

    @Test
    fun permanentFailureIsReported() = runTest {
        val ids = MutableStateFlow<String?>(null)
        val error = IllegalStateException("boom")
        val failures = mutableListOf<Throwable>()
        val poller = InvoicePaymentPoller { throw error }

        val job = launch {
            poller.awaitPayment(ids, onPaid = {}, onFailure = { failures += it })
        }

        ids.value = "inv-1"
        runCurrent()

        Assert.assertEquals(listOf(error), failures)

        job.cancelAndJoin()
    }
}
