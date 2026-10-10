package org.btcmap.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

/** The sync card's duration formatting, across its three ranges. */
class SyncDurationFormatTest {

    @Test
    fun formatsMillisecondsSecondsAndMinutes() {
        assertEquals("0 ms", formatSyncDuration(0.milliseconds))
        assertEquals("999 ms", formatSyncDuration(999.milliseconds))
        assertEquals("1.0 s", formatSyncDuration(1_000.milliseconds))
        assertEquals("59.9 s", formatSyncDuration(59_900.milliseconds))
        assertEquals("1m 0s", formatSyncDuration(60_000.milliseconds))
        assertEquals("2m 5s", formatSyncDuration(125_000.milliseconds))
    }
}
