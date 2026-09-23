package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class BoostApiTest : ApiTestBase() {
    @Test
    fun getPlaceBoostQuote_parsesQuotes() = runTest {
        enqueueJson("""{"quote_30d_sat":5000,"quote_90d_sat":10000,"quote_365d_sat":30000}""")

        val quote = api().getPlaceBoostQuote()

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/place-boosts/quote", request.url.encodedPath)
        Assert.assertEquals(5000L, quote.quote30dsat)
        Assert.assertEquals(10000L, quote.quote90dsat)
        Assert.assertEquals(30000L, quote.quote365dsat)
    }

    @Test
    fun boostPlace_postsOrderAndParsesInvoice() = runTest {
        enqueueJson("""{"invoice_id":"inv-1","invoice":"lnbc1..."}""")

        val response = api().boostPlace(placeId = 42, days = 30)

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/place-boosts", request.url.encodedPath)
        Assert.assertEquals("""{"place_id":"42","days":30}""", request.jsonBody())
        Assert.assertEquals("inv-1", response.invoiceId)
        Assert.assertEquals("lnbc1...", response.invoice)
    }

    @Test
    fun boostPlace_sendsTheSelectedDuration() = runTest {
        enqueueJson("""{"invoice_id":"inv-2","invoice":"lnbc2..."}""")

        api().boostPlace(placeId = 42, days = 365)

        val request = takeRequest()
        Assert.assertEquals("""{"place_id":"42","days":365}""", request.jsonBody())
    }
}
