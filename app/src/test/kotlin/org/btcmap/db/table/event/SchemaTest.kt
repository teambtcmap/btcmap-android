package org.btcmap.db.table.event

import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaTest {
    @Test
    fun name() {
        assertEquals("event", TABLE)
    }

    @Test
    fun columns() {
        assertEquals("id", ID)
        assertEquals("area_id", AREA_ID)
        assertEquals("lat", LAT)
        assertEquals("lon", LON)
        assertEquals("name", NAME)
        assertEquals("website", WEBSITE)
        assertEquals("starts_at", STARTS_AT)
        assertEquals("ends_at", ENDS_AT)
        assertEquals("cron_schedule", CRON_SCHEDULE)
    }

    @Test
    fun create() {
        assert(CREATE.contains(TABLE))
        assert(CREATE.contains(ID))
        assert(CREATE.contains(AREA_ID))
        assert(CREATE.contains(LAT))
        assert(CREATE.contains(LON))
        assert(CREATE.contains(NAME))
        assert(CREATE.contains(WEBSITE))
        assert(CREATE.contains(STARTS_AT))
        assert(CREATE.contains(ENDS_AT))
        assert(CREATE.contains(CRON_SCHEDULE))
    }
}