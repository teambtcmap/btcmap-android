package org.btcmap.boost

import org.btcmap.R
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
    fun fromButtonId_mapsEachOptionToItsDuration() {
        Assert.assertEquals(
            BoostDuration.ONE_MONTH,
            BoostDuration.fromButtonId(R.id.boost_1m),
        )
        Assert.assertEquals(
            BoostDuration.THREE_MONTHS,
            BoostDuration.fromButtonId(R.id.boost_3m),
        )
        Assert.assertEquals(
            BoostDuration.TWELVE_MONTHS,
            BoostDuration.fromButtonId(R.id.boost_12m),
        )
        Assert.assertEquals(30L, BoostDuration.ONE_MONTH.days)
        Assert.assertEquals(90L, BoostDuration.THREE_MONTHS.days)
        Assert.assertEquals(365L, BoostDuration.TWELVE_MONTHS.days)
    }

    @Test
    fun fromButtonId_returnsNullForAnUnknownOption() {
        Assert.assertNull(BoostDuration.fromButtonId(R.id.duration_options))
        Assert.assertNull(BoostDuration.fromButtonId(0))
    }

    @Test
    fun priceSat_readsTheQuoteForTheDuration() {
        Assert.assertEquals(5000L, BoostDuration.ONE_MONTH.priceSat(quote))
        Assert.assertEquals(10000L, BoostDuration.THREE_MONTHS.priceSat(quote))
        Assert.assertEquals(30000L, BoostDuration.TWELVE_MONTHS.priceSat(quote))
    }
}
