package org.btcmap.boost

import org.btcmap.api.PlaceBoostQuoteResponse
import org.junit.Assert
import org.junit.Test

class BoostPlanTest {

    private val quote = PlaceBoostQuoteResponse(
        quote30dSat = 5000L,
        quote90dSat = 10000L,
        quote365dSat = 30000L,
    )

    @Test
    fun days_matchesEachDuration() {
        Assert.assertEquals(30L, BoostPlan.ONE_MONTH.days)
        Assert.assertEquals(90L, BoostPlan.THREE_MONTHS.days)
        Assert.assertEquals(365L, BoostPlan.TWELVE_MONTHS.days)
    }

    @Test
    fun priceSat_readsTheQuoteForTheDuration() {
        Assert.assertEquals(5000L, BoostPlan.ONE_MONTH.priceSat(quote))
        Assert.assertEquals(10000L, BoostPlan.THREE_MONTHS.priceSat(quote))
        Assert.assertEquals(30000L, BoostPlan.TWELVE_MONTHS.priceSat(quote))
    }
}
