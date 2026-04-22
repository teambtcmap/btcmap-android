package org.btcmap.db.table.user

import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaTest {
    @Test
    fun name() {
        assertEquals("user", TABLE)
    }

    @Test
    fun columns() {
        assertEquals("id", ID)
        assertEquals("name", NAME)
        assertEquals("roles", ROLES)
        assertEquals("saved_places", SAVED_PLACES)
        assertEquals("saved_areas", SAVED_AREAS)
    }

    @Test
    fun create() {
        assert(CREATE.contains(TABLE))
        assert(CREATE.contains(ID))
        assert(CREATE.contains(NAME))
        assert(CREATE.contains(ROLES))
        assert(CREATE.contains(SAVED_PLACES))
        assert(CREATE.contains(SAVED_AREAS))
    }
}