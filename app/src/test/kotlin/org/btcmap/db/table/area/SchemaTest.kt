package org.btcmap.db.table.area

import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaTest {
    @Test
    fun name() {
        assertEquals("area", TABLE)
    }

    @Test
    fun columns() {
        assertEquals("id", ID)
        assertEquals("name", NAME)
        assertEquals("type", TYPE)
        assertEquals("url_alias", URL_ALIAS)
        assertEquals("icon", ICON)
        assertEquals("icon_wide", ICON_WIDE)
        assertEquals("website_url", WEBSITE_URL)
        assertEquals("description", DESCRIPTION)
        assertEquals("bbox_west", BBOX_WEST)
        assertEquals("bbox_south", BBOX_SOUTH)
        assertEquals("bbox_east", BBOX_EAST)
        assertEquals("bbox_north", BBOX_NORTH)
        assertEquals("updated_at", UPDATED_AT)
        assertEquals("deleted_at", DELETED_AT)
    }

    @Test
    fun create() {
        assert(CREATE.contains(TABLE))
        assert(CREATE.contains(ID))
        assert(CREATE.contains(NAME))
        assert(CREATE.contains(TYPE))
        assert(CREATE.contains(URL_ALIAS))
        assert(CREATE.contains(ICON))
        assert(CREATE.contains(ICON_WIDE))
        assert(CREATE.contains(WEBSITE_URL))
        assert(CREATE.contains(DESCRIPTION))
        assert(CREATE.contains(BBOX_WEST))
        assert(CREATE.contains(BBOX_SOUTH))
        assert(CREATE.contains(BBOX_EAST))
        assert(CREATE.contains(BBOX_NORTH))
        assert(CREATE.contains(UPDATED_AT))
        assert(CREATE.contains(DELETED_AT))
    }
}
