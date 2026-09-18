package org.btcmap.map

import org.btcmap.db.table.event.Event
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class EventUpcomingTest {
    private val now = ZonedDateTime.parse("2026-06-01T00:00:00Z")

    private fun event(startsAt: String) = Event(
        id = 1L,
        areaId = null,
        lat = 0.0,
        lon = 0.0,
        name = "Event",
        website = null,
        startsAt = ZonedDateTime.parse(startsAt),
        endsAt = null,
    )

    @Test
    fun futureEventIsUpcoming() {
        Assert.assertTrue(event("2026-06-02T00:00:00Z").isUpcomingOrUndated(now))
    }

    @Test
    fun pastEventIsNotUpcoming() {
        Assert.assertFalse(event("2026-05-31T00:00:00Z").isUpcomingOrUndated(now))
    }

    @Test
    fun undatedEventIsKept() {
        Assert.assertTrue(event("1970-01-01T00:00:00Z").isUpcomingOrUndated(now))
    }
}
