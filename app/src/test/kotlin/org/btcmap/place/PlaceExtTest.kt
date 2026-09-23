package org.btcmap.place

import org.btcmap.db.table.place.Place
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class PlaceExtTest {

    private val now = ZonedDateTime.parse("2026-06-01T00:00:00Z")

    @Test
    fun isBoosted_trueForFutureBoost() {
        val place = place(boostedUntil = ZonedDateTime.parse("2026-06-02T00:00:00Z"))

        Assert.assertTrue(place.isBoosted(now))
    }

    @Test
    fun isBoosted_falseForExpiredBoost() {
        val place = place(boostedUntil = ZonedDateTime.parse("2026-05-31T23:59:59Z"))

        Assert.assertFalse(place.isBoosted(now))
    }

    @Test
    fun isBoosted_falseWhenBoostIsNull() {
        Assert.assertFalse(place(boostedUntil = null).isBoosted(now))
    }

    private fun place(boostedUntil: ZonedDateTime?): Place {
        return Place(
            id = 1,
            updatedAt = ZonedDateTime.parse("2026-01-01T00:00:00Z"),
            lat = 0.0,
            lon = 0.0,
            icon = "store",
            name = "Test",
            localizedName = null,
            verifiedAt = null,
            address = null,
            openingHours = null,
            localizedOpeningHours = null,
            phone = null,
            website = null,
            email = null,
            twitter = null,
            facebook = null,
            instagram = null,
            line = null,
            requiredAppUrl = null,
            boostedUntil = boostedUntil,
            comments = null,
            telegram = null,
            osmId = null,
        )
    }
}
