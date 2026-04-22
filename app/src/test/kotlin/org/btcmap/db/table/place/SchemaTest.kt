package org.btcmap.db.table.place

import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaTest {
    @Test
    fun name() {
        assertEquals("place", TABLE)
    }

    @Test
    fun columns() {
        assertEquals("id", ID)
        assertEquals("bundled", BUNDLED)
        assertEquals("updated_at", UPDATED_AT)
        assertEquals("lat", LAT)
        assertEquals("lon", LON)
        assertEquals("icon", ICON)
        assertEquals("name", NAME)
        assertEquals("localized_name", LOCALIZED_NAME)
        assertEquals("verified_at", VERIFIED_AT)
        assertEquals("address", ADDRESS)
        assertEquals("opening_hours", OPENING_HOURS)
        assertEquals("localized_opening_hours", LOCALIZED_OPENING_HOURS)
        assertEquals("phone", PHONE)
        assertEquals("website", WEBSITE)
        assertEquals("email", EMAIL)
        assertEquals("twitter", TWITTER)
        assertEquals("facebook", FACEBOOK)
        assertEquals("instagram", INSTAGRAM)
        assertEquals("line", LINE)
        assertEquals("required_app_url", REQUIRED_APP_URL)
        assertEquals("boosted_until", BOOSTED_UNTIL)
        assertEquals("comments", COMMENTS)
        assertEquals("telegram", TELEGRAM)
    }

    @Test
    fun create() {
        assert(CREATE.contains(TABLE))
        assert(CREATE.contains(ID))
        assert(CREATE.contains(BUNDLED))
        assert(CREATE.contains(UPDATED_AT))
        assert(CREATE.contains(LAT))
        assert(CREATE.contains(LON))
        assert(CREATE.contains(ICON))
        assert(CREATE.contains(NAME))
        assert(CREATE.contains(LOCALIZED_NAME))
        assert(CREATE.contains(VERIFIED_AT))
        assert(CREATE.contains(ADDRESS))
        assert(CREATE.contains(OPENING_HOURS))
        assert(CREATE.contains(LOCALIZED_OPENING_HOURS))
        assert(CREATE.contains(PHONE))
        assert(CREATE.contains(WEBSITE))
        assert(CREATE.contains(EMAIL))
        assert(CREATE.contains(TWITTER))
        assert(CREATE.contains(FACEBOOK))
        assert(CREATE.contains(INSTAGRAM))
        assert(CREATE.contains(LINE))
        assert(CREATE.contains(REQUIRED_APP_URL))
        assert(CREATE.contains(BOOSTED_UNTIL))
        assert(CREATE.contains(COMMENTS))
        assert(CREATE.contains(TELEGRAM))
    }
}