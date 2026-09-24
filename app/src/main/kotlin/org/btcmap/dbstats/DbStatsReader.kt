package org.btcmap.dbstats

import androidx.sqlite.SQLiteConnection
import kotlin.use

/** Reads per-table statistics from a database. */
class DbStatsReader(private val conn: SQLiteConnection) {

    /** Lists the user tables and their row stats, ordered by name. */
    fun readTables(): List<TableStats> {
        val tableNames = mutableListOf<String>()
        conn.prepare(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table' AND name NOT LIKE 'sqlite_%'
                AND name != 'android_metadata'
            ORDER BY name;
            """
        ).use {
            while (it.step()) {
                tableNames.add(it.getText(0))
            }
        }

        return tableNames.map(::readTable)
    }

    /** The schema version stored in the database's `user_version` pragma. */
    fun readUserVersion(): Int {
        conn.prepare("SELECT user_version FROM pragma_user_version;").use {
            return if (it.step()) it.getInt(0) else 0
        }
    }

    private fun readTable(name: String): TableStats {
        val table = quote(name)
        val columns = readColumns(name)

        val rowCount = count("SELECT count(*) FROM $table;")

        val hasDeletedAt = DELETED_AT in columns
        val hasUpdatedAt = UPDATED_AT in columns

        // Upcoming events are counted independently of the sync fields: a table
        // with starts_at but neither deleted_at nor updated_at still reports a
        // future count. This is the SQL equivalent of the shared isUpcoming rule
        // (a start strictly after now), so an event with no real start date is
        // not counted.
        val futureRowCount = if (STARTS_AT in columns) {
            val notDeleted = if (hasDeletedAt) " AND $DELETED_AT IS NULL" else ""
            count(
                "SELECT count(*) FROM $table " +
                    "WHERE $STARTS_AT IS NOT NULL " +
                    "AND julianday($STARTS_AT) > julianday('now')$notDeleted;"
            )
        } else {
            null
        }

        if (!hasDeletedAt && !hasUpdatedAt) {
            // Not a sync-tracked table (no tombstones, no cursor): report the
            // size only.
            return TableStats(
                name = name,
                rowCount = rowCount,
                visibleRowCount = null,
                deletedRowCount = null,
                maxUpdatedAt = null,
                futureRowCount = futureRowCount,
            )
        }

        val deletedRowCount = if (hasDeletedAt) {
            count("SELECT count(*) FROM $table WHERE $DELETED_AT IS NOT NULL;")
        } else {
            // A table without deleted_at simply has no tombstones.
            0L
        }

        val maxUpdatedAt = if (hasUpdatedAt) {
            // julianday, not max(): timestamps are stored as text and
            // ZonedDateTime.toString() is not fixed-width, so text ordering
            // is not chronological.
            conn.prepare(
                """
                SELECT $UPDATED_AT FROM $table
                WHERE $UPDATED_AT IS NOT NULL
                ORDER BY julianday($UPDATED_AT) DESC
                LIMIT 1;
                """
            ).use {
                if (it.step()) it.getText(0) else null
            }
        } else {
            null
        }

        return TableStats(
            name = name,
            rowCount = rowCount,
            visibleRowCount = rowCount - deletedRowCount,
            deletedRowCount = deletedRowCount,
            maxUpdatedAt = maxUpdatedAt,
            futureRowCount = futureRowCount,
        )
    }

    /**
     * Reads the column names of [name] through a bound parameter rather than
     * interpolating the name, so a table name from `sqlite_master` that needs
     * escaping cannot break out of the statement.
     */
    private fun readColumns(name: String): Set<String> {
        val columns = mutableSetOf<String>()
        conn.prepare("SELECT name FROM pragma_table_info(?);").use { stmt ->
            stmt.bindText(1, name)
            while (stmt.step()) {
                columns.add(stmt.getText(0))
            }
        }
        return columns
    }

    /** Quotes a SQL identifier, doubling any embedded double quote. */
    private fun quote(name: String): String = "\"" + name.replace("\"", "\"\"") + "\""

    private fun count(sql: String): Long {
        conn.prepare(sql).use {
            it.step()
            return it.getLong(0)
        }
    }

    private companion object {
        const val DELETED_AT = "deleted_at"
        const val UPDATED_AT = "updated_at"
        const val STARTS_AT = "starts_at"
    }
}
