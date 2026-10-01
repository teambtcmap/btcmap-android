package org.btcmap.util

import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class ZonedDateTimeExtTest {

    private val now = ZonedDateTime.parse("2026-06-01T00:00:00Z")

    @Test
    fun isUpcoming_futureStart_isTrue() {
        Assert.assertTrue(ZonedDateTime.parse("2026-06-02T00:00:00Z").isUpcoming(now))
    }

    @Test
    fun isUpcoming_startExactlyNow_isFalse() {
        Assert.assertFalse(now.isUpcoming(now))
    }

    @Test
    fun isUpcoming_pastStart_isFalse() {
        Assert.assertFalse(ZonedDateTime.parse("2026-05-31T00:00:00Z").isUpcoming(now))
    }

    @Test
    fun isUpcoming_epochPlaceholder_isFalse() {
        // The API represents an event without a start date as the epoch. Treated
        // as the ordinary timestamp it is, that start is simply in the past.
        Assert.assertFalse(ZonedDateTime.parse("1970-01-01T00:00:00Z").isUpcoming(now))
    }
}
