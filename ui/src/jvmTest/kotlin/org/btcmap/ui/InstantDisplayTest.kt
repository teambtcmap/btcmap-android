package org.btcmap.ui

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class InstantDisplayTest {

    @Test
    fun format_withADateFormatter_rendersTheSystemZonesDate() {
        // A date formatter needs a zone to derive the calendar date, so an
        // instant rendered without one would throw (a java.time.Instant has no
        // date fields).
        val instant = Instant.parse("2026-03-15T23:30:00Z")
        val expected = java.time.Instant.ofEpochSecond(instant.epochSeconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        assertEquals(
            expected.toString(),
            instant.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
        )
    }

    @Test
    fun format_withALocalizedDateFormatter_doesNotThrow() {
        val instant = Instant.parse("2026-03-15T09:30:00Z")

        instant.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}
