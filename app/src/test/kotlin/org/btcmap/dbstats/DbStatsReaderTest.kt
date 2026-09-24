package org.btcmap.dbstats

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Assert
import org.junit.Test

class DbStatsReaderTest {

    @Test
    fun readUserVersion_readsTheStoredSchemaVersion() {
        withConnection { conn ->
            conn.execSQL("PRAGMA user_version=42;")

            Assert.assertEquals(42, DbStatsReader(conn).readUserVersion())
        }
    }

    @Test
    fun readUserVersion_defaultsToZeroForANewDatabase() {
        withConnection { conn ->
            Assert.assertEquals(0, DbStatsReader(conn).readUserVersion())
        }
    }

    @Test
    fun readTables_listsTablesOrderedByName() {
        withConnection { conn ->
            conn.execSQL("CREATE TABLE place (id INTEGER PRIMARY KEY);")
            conn.execSQL("CREATE TABLE comment (id INTEGER PRIMARY KEY);")

            val tables = DbStatsReader(conn).readTables()

            Assert.assertEquals(listOf("comment", "place"), tables.map { it.name })
        }
    }

    @Test
    fun readTables_ignoresInternalTables() {
        withConnection { conn ->
            // AUTOINCREMENT creates the internal sqlite_sequence table.
            conn.execSQL(
                "CREATE TABLE place (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT);",
            )
            conn.execSQL("INSERT INTO place (name) VALUES ('a');")

            val tables = DbStatsReader(conn).readTables()

            Assert.assertEquals(listOf("place"), tables.map { it.name })
        }
    }

    @Test
    fun readTables_ignoresAndroidMetadata() {
        withConnection { conn ->
            // AndroidSQLiteDriver creates android_metadata to store the locale.
            conn.execSQL("CREATE TABLE android_metadata (locale TEXT);")
            conn.execSQL("CREATE TABLE place (id INTEGER PRIMARY KEY);")

            val tables = DbStatsReader(conn).readTables()

            Assert.assertEquals(listOf("place"), tables.map { it.name })
        }
    }

    @Test
    fun readTables_omitsCountSplitForTablesWithoutTrackingFields() {
        withConnection { conn ->
            conn.execSQL("CREATE TABLE user (id INTEGER PRIMARY KEY, name TEXT);")
            conn.execSQL("INSERT INTO user (name) VALUES ('a'), ('b'), ('c');")

            val table = DbStatsReader(conn).readTables().single()

            Assert.assertEquals(3L, table.rowCount)
            Assert.assertNull(table.visibleRowCount)
            Assert.assertNull(table.deletedRowCount)
            Assert.assertNull(table.maxUpdatedAt)
            Assert.assertNull(table.futureRowCount)
        }
    }

    @Test
    fun readTables_treatsMissingDeletedAtAsAllVisibleWhenUpdatedAtPresent() {
        withConnection { conn ->
            conn.execSQL(
                "CREATE TABLE place (id INTEGER PRIMARY KEY, name TEXT, updated_at TEXT);",
            )
            conn.execSQL(
                "INSERT INTO place (name, updated_at) VALUES ('a', '2024-01-01T00:00:00Z'), ('b', '2024-01-02T00:00:00Z'), ('c', '2024-01-03T00:00:00Z');",
            )

            val table = DbStatsReader(conn).readTables().single()

            Assert.assertEquals(3L, table.rowCount)
            Assert.assertEquals(3L, table.visibleRowCount)
            Assert.assertEquals(0L, table.deletedRowCount)
            Assert.assertEquals("2024-01-03T00:00:00Z", table.maxUpdatedAt)
        }
    }

    @Test
    fun readTables_splitsVisibleAndDeletedRowsWhenDeletedAtIsPresent() {
        withConnection { conn ->
            conn.execSQL(
                "CREATE TABLE comment (id INTEGER PRIMARY KEY, comment TEXT, deleted_at TEXT);",
            )
            conn.execSQL(
                """
                INSERT INTO comment (comment, deleted_at) VALUES
                    ('kept', NULL),
                    ('removed', '2024-01-02T00:00:00Z'),
                    ('kept too', NULL);
                """,
            )

            val table = DbStatsReader(conn).readTables().single()

            Assert.assertEquals(3L, table.rowCount)
            Assert.assertEquals(2L, table.visibleRowCount)
            Assert.assertEquals(1L, table.deletedRowCount)
        }
    }

    @Test
    fun readTables_readsMaxUpdatedAt() {
        withConnection { conn ->
            conn.execSQL(
                "CREATE TABLE event (id INTEGER PRIMARY KEY, updated_at TEXT);",
            )
            conn.execSQL(
                """
                INSERT INTO event (updated_at) VALUES
                    ('2024-01-01T00:00:00Z'),
                    ('2024-01-03T00:00:00Z'),
                    ('2024-01-02T00:00:00Z');
                """,
            )

            val table = DbStatsReader(conn).readTables().single()

            Assert.assertEquals("2024-01-03T00:00:00Z", table.maxUpdatedAt)
        }
    }

    @Test
    fun readTables_comparesUpdatedAtChronologicallyNotAsText() {
        withConnection { conn ->
            conn.execSQL(
                "CREATE TABLE event (id INTEGER PRIMARY KEY, updated_at TEXT);",
            )
            // ZonedDateTime.toString() drops a zero fraction, so as text the
            // earlier "2024-01-01T10:00Z" sorts after the later ".500Z".
            conn.execSQL(
                """
                INSERT INTO event (updated_at) VALUES
                    ('2024-01-01T10:00Z'),
                    ('2024-01-01T10:00:00.500Z');
                """,
            )

            val table = DbStatsReader(conn).readTables().single()

            Assert.assertEquals("2024-01-01T10:00:00.500Z", table.maxUpdatedAt)
        }
    }

    @Test
    fun readTables_returnsNullMaxUpdatedAtForEmptyTable() {
        withConnection { conn ->
            conn.execSQL(
                "CREATE TABLE event (id INTEGER PRIMARY KEY, updated_at TEXT);",
            )

            val table = DbStatsReader(conn).readTables().single()

            Assert.assertEquals(0L, table.rowCount)
            Assert.assertNull(table.maxUpdatedAt)
        }
    }

    @Test
    fun readTables_countsFutureEvents() {
        withConnection { conn ->
            conn.execSQL(
                "CREATE TABLE event (id INTEGER PRIMARY KEY, starts_at TEXT, updated_at TEXT, deleted_at TEXT);",
            )
            conn.execSQL(
                """
                INSERT INTO event (starts_at, updated_at, deleted_at) VALUES
                    ('2999-01-01T00:00:00Z', '2024-01-01T00:00:00Z', NULL),
                    ('2000-01-01T00:00:00Z', '2024-01-01T00:00:00Z', NULL),
                    ('1970-01-01T00:00:00Z', '2024-01-01T00:00:00Z', NULL),
                    ('2999-02-01T00:00:00Z', '2024-01-01T00:00:00Z', '2024-01-02T00:00:00Z');
                """,
            )

            val table = DbStatsReader(conn).readTables().single()

            // Only the first row: the past and the epoch placeholder are not
            // upcoming, and the deleted future event is not visible.
            Assert.assertEquals(1L, table.futureRowCount)
        }
    }

    private fun withConnection(block: (SQLiteConnection) -> Unit) {
        val conn = BundledSQLiteDriver().open(":memory:")
        try {
            block(conn)
        } finally {
            conn.close()
        }
    }
}
