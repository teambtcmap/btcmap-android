package org.btcmap.db

import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.junit.Assert
import org.junit.Test

class SQLiteStatementExtTest {

    @Test
    fun getJsonObjectOrNull_readsAValidObject() {
        withStatement("""{"a":1}""") { stmt ->
            val json = stmt.getJsonObjectOrNull(0)

            Assert.assertNotNull(json)
            Assert.assertEquals(1, json!!.get("a").asInt)
        }
    }

    @Test
    fun getJsonObjectOrNull_returnsNullForMalformedText() {
        // A malformed value must read as absent rather than throw out of a read.
        withStatement("{not json") { stmt ->
            Assert.assertNull(stmt.getJsonObjectOrNull(0))
        }
    }

    @Test
    fun getJsonObjectOrNull_returnsNullForNull() {
        withStatement(null) { stmt ->
            Assert.assertNull(stmt.getJsonObjectOrNull(0))
        }
    }

    private fun withStatement(value: String?, block: (SQLiteStatement) -> Unit) {
        val db = Database(BundledSQLiteDriver(), ":memory:")
        try {
            db.conn.prepare("SELECT ?1;").use { stmt ->
                if (value == null) stmt.bindNull(1) else stmt.bindText(1, value)
                stmt.step()
                block(stmt)
            }
        } finally {
            db.close()
        }
    }
}
