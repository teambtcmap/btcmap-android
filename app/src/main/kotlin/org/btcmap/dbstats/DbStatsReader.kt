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
        val columns = mutableSetOf<String>()
        conn.prepare("SELECT name FROM pragma_table_info('$name');").use {
            while (it.step()) {
                columns.add(it.getText(0))
            }
        }

        val rowCount = count("SELECT count(*) FROM \"$name\";")

        val hasDeletedAt = DELETED_AT in columns
        val hasUpdatedAt = UPDATED_AT in columns
        if (!hasDeletedAt && !hasUpdatedAt) {
            // Not a sync-tracked table (no tombstones, no cursor): report the
            // size only.
            return TableStats(
                name = name,
                rowCount = rowCount,
                visibleRowCount = null,
                deletedRowCount = null,
                maxUpdatedAt = null,
                futureRowCount = null,
            )
        }

        val deletedRowCount = if (hasDeletedAt) {
            count("SELECT count(*) FROM \"$name\" WHERE $DELETED_AT IS NOT NULL;")
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
                SELECT $UPDATED_AT FROM "$name"
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

        val futureRowCount = if (STARTS_AT in columns) {
            // The events table: how many visible events have not started yet.
            // julianday compares the stored ISO-8601 text to now, so the epoch
            // sentinel the API uses for undated events is not counted as future.
            val notDeleted = if (hasDeletedAt) " AND $DELETED_AT IS NULL" else ""
            count(
                "SELECT count(*) FROM \"$name\" " +
                    "WHERE $STARTS_AT IS NOT NULL " +
                    "AND julianday($STARTS_AT) > julianday('now')$notDeleted;"
            )
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
