package org.btcmap.db.table.preference

import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaTest {
    @Test
    fun name() {
        assertEquals("preference", TABLE)
    }

    @Test
    fun columns() {
        assertEquals("pref_key", KEY)
        assertEquals("pref_value", VALUE)
    }

    @Test
    fun create() {
        assert(CREATE.contains(TABLE))
        assert(CREATE.contains(KEY))
        assert(CREATE.contains(VALUE))
    }
}
