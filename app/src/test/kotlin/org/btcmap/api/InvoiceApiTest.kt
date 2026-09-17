package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

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
}
