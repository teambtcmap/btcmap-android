package org.btcmap.util

import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant

class InstantExtTest {

    private val now = Instant.parse("2026-06-01T00:00:00Z")

    @Test
    fun isUpcoming_futureStart_isTrue() {
        Assert.assertTrue(Instant.parse("2026-06-02T00:00:00Z").isUpcoming(now))
    }

    @Test
    fun isUpcoming_startExactlyNow_isFalse() {
        Assert.assertFalse(now.isUpcoming(now))
    }

    @Test
    fun isUpcoming_pastStart_isFalse() {
        Assert.assertFalse(Instant.parse("2026-05-31T00:00:00Z").isUpcoming(now))
    }

    @Test
    fun isUpcoming_epochPlaceholder_isFalse() {
        // The API represents an event without a start date as the epoch. Treated
        // as the ordinary timestamp it is, that start is simply in the past.
        Assert.assertFalse(Instant.parse("1970-01-01T00:00:00Z").isUpcoming(now))
    }

    @Test
    fun toInstant_withOmittedZeroSeconds_parsesMidnight() {
        // Rust's `time` crate drops the seconds when they are zero, so the API
        // sends `...T00:00Z`; it is a valid timestamp, not a malformed one.
        Assert.assertEquals(
            Instant.parse("2025-02-03T00:00:00Z"),
            "2025-02-03T00:00Z".toInstant(),
        )
    }

    @Test
    fun toInstant_withOmittedZeroSecondsAndOffset_parses() {
        Assert.assertEquals(
            Instant.parse("2025-02-03T00:00:00+02:00"),
            "2025-02-03T00:00+02:00".toInstant(),
        )
    }

    @Test
    fun toInstant_withSeconds_isUnchanged() {
        Assert.assertEquals(
            Instant.parse("2025-02-03T04:05:06.789Z"),
            "2025-02-03T04:05:06.789Z".toInstant(),
        )
    }

    @Test
    fun toInstantOrNull_withOmittedZeroSeconds_parsesMidnight() {
        Assert.assertEquals(
            Instant.parse("2025-02-03T00:00:00Z"),
            "2025-02-03T00:00Z".toInstantOrNull(),
        )
    }

    @Test
    fun toInstantOrNull_malformed_isNull() {
        Assert.assertNull("not-an-instant".toInstantOrNull())
    }
}
