package org.btcmap.db.table.comment

import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaTest {
    @Test
    fun name() {
        assertEquals("comment", TABLE)
    }

    @Test
    fun columns() {
        assertEquals("id", ID)
        assertEquals("place_id", PLACE_ID)
        assertEquals("comment", COMMENT)
        assertEquals("created_at", CREATED_AT)
        assertEquals("updated_at", UPDATED_AT)
    }

    @Test
    fun create() {
        assert(CREATE.contains(TABLE))
        assert(CREATE.contains(ID))
        assert(CREATE.contains(PLACE_ID))
        assert(CREATE.contains(COMMENT))
        assert(CREATE.contains(CREATED_AT))
        assert(CREATE.contains(UPDATED_AT))
    }
}