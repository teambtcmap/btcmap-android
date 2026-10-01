package org.btcmap.boost

import org.btcmap.api.PlaceBoostQuoteResponse
import org.junit.Assert
import org.junit.Test

class BoostDurationTest {

    private val quote = PlaceBoostQuoteResponse(
        quote30dSat = 5000L,
        quote90dSat = 10000L,
        quote365dSat = 30000L,
    )

    @Test
    fun days_matchesEachDuration() {
        Assert.assertEquals(30L, BoostDuration.ONE_MONTH.days)
        Assert.assertEquals(90L, BoostDuration.THREE_MONTHS.days)
        Assert.assertEquals(365L, BoostDuration.TWELVE_MONTHS.days)
    }

    @Test
    fun priceSat_readsTheQuoteForTheDuration() {
        Assert.assertEquals(5000L, BoostDuration.ONE_MONTH.priceSat(quote))
        Assert.assertEquals(10000L, BoostDuration.THREE_MONTHS.priceSat(quote))
        Assert.assertEquals(30000L, BoostDuration.TWELVE_MONTHS.priceSat(quote))
    }
}
