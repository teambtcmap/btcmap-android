package org.btcmap.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvoiceApiTest : ApiTestBase() {
    @Test
    fun getInvoice_parsesPaidInvoice() = runTest {
        enqueueJson("""{"id":"inv-1","status":"paid"}""")

        val invoice = api().getInvoice("inv-1")

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/invoices/inv-1", request.url.encodedPath)
        Assert.assertEquals("inv-1", invoice.id)
        Assert.assertEquals("paid", invoice.status)
        Assert.assertTrue(invoice.paid)
    }

    @Test
    fun paid_isFalseForUnpaidInvoice() = runTest {
        enqueueJson("""{"id":"inv-2","status":"unpaid"}""")

        val invoice = api().getInvoice("inv-2")

        Assert.assertFalse(invoice.paid)
    }

    @Test
    fun awaitPaidInvoice_pollsUntilPaid() = runTest {
        enqueueJson("""{"id":"inv-1","status":"unpaid"}""")
        enqueueJson("""{"id":"inv-1","status":"unpaid"}""")
        enqueueJson("""{"id":"inv-1","status":"paid"}""")

        val invoice = api().awaitPaidInvoice("inv-1")

        Assert.assertTrue(invoice.paid)
        Assert.assertEquals(3, server.requestCount)
    }

    @Test
    fun awaitPaidInvoice_retriesTransientFailures() = runTest {
        enqueueJson("""{"message":"boom"}""", code = 500)
        enqueueJson("""{"id":"inv-1","status":"paid"}""")

        val invoice = api().awaitPaidInvoice("inv-1")

        Assert.assertTrue(invoice.paid)
        Assert.assertEquals(2, server.requestCount)
    }

    @Test
    fun awaitPaidInvoice_doesNotRetryPermanentError() = runTest {
        enqueueJson("""{"message":"Unknown invoice"}""", code = 404)
        // A retry would pick this up, so the loop must never issue it.
        enqueueJson("""{"id":"inv-1","status":"paid"}""")

        try {
            api().awaitPaidInvoice("inv-1")
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(404, e.code)
        }

        Assert.assertEquals(1, server.requestCount)
    }

    @Test
    fun awaitPaidInvoice_doesNotRetryParseFailure() = runTest {
        enqueueJson("not json")
        enqueueJson("""{"id":"inv-1","status":"paid"}""")

        try {
            api().awaitPaidInvoice("inv-1")
            Assert.fail("Expected ApiParseException")
        } catch (e: ApiParseException) {
            // expected
        }

        Assert.assertEquals(1, server.requestCount)
    }

    @Test
    fun awaitPaidInvoice_propagatesCancellation() = runTest {
        // Keep the invoice unpaid so the loop would poll forever if cancellation
        // were swallowed.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("""{"id":"inv-1","status":"unpaid"}""")
                    .build()
        }

        val job = launch { api().awaitPaidInvoice("inv-1") }
        runCurrent()
        job.cancelAndJoin()

        Assert.assertTrue(job.isCancelled)
    }
}
