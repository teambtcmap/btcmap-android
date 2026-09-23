package org.btcmap.db.table.event

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindHttpUrlOrNull
import org.btcmap.db.bindLongOrNull
import org.btcmap.db.bindZonedDateTime
import org.btcmap.db.bindZonedDateTimeOrNull
import org.btcmap.db.escapeLikePattern
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

class EventQueries(private val conn: SQLiteConnection) {
    fun insert(rows: List<Event>) {
        if (rows.isEmpty()) return

        // OR REPLACE, not a plain INSERT: the delta sync can return the same
        // event more than once (a soft delete bumps updated_at, and a page can
        // be re-read while widening the window), so a plain insert would abort
        // the whole sync transaction with a primary-key conflict.
        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE ($ID, $AREA_ID, $LAT, $LON, $NAME, $WEBSITE, $STARTS_AT, $ENDS_AT, $UPDATED_AT, $DELETED_AT)
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10);
            """
        ).use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindLongOrNull(2, row.areaId)
                stmt.bindDouble(3, row.lat)
                stmt.bindDouble(4, row.lon)
                stmt.bindText(5, row.name)
                stmt.bindHttpUrlOrNull(6, row.website)
                stmt.bindZonedDateTime(7, row.startsAt)
                stmt.bindZonedDateTimeOrNull(8, row.endsAt)
                stmt.bindZonedDateTime(9, row.updatedAt)
                stmt.bindZonedDateTimeOrNull(10, row.deletedAt)
                stmt.step()
                stmt.reset()
            }
        }
    }

    fun selectAll(): List<Event> {
        conn.prepare(
            """
            SELECT ${FullProjection.COLUMNS}
            FROM $TABLE
            WHERE $DELETED_AT IS NULL;
            """
        ).use {
            val rows = mutableListOf<Event>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectByBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<Event> {
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE $LAT >= ?1 AND $LAT <= ?2 AND $LON >= ?3 AND $LON <= ?4
                    AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)
            val rows = mutableListOf<Event>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectById(id: Long): Event? {
        conn.prepare(
            """
            SELECT ${FullProjection.COLUMNS}
            FROM $TABLE
            WHERE $ID = ?1 AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindLong(1, id)
            if (it.step()) {
                return FullProjection.fromStatement(it)
            }
            return null
        }
    }

    fun selectBySearchString(searchString: String): List<Event> {
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE UPPER($NAME) LIKE '%' || UPPER(?1) || '%' ESCAPE '\'
                    AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindText(1, searchString.escapeLikePattern())
            val rows = mutableListOf<Event>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        // julianday, not plain max(): timestamps are stored as text and
        // ZonedDateTime.toString() is not fixed-width (it drops a zero second
        // and a zero fraction), so text ordering is not chronological.
        conn.prepare(
            """
            SELECT $UPDATED_AT
            FROM $TABLE
            ORDER BY julianday($UPDATED_AT) DESC
            LIMIT 1;
            """
        ).use {
            if (!it.step()) {
                return null
            }
            return it.getZonedDateTimeOrNull(0)
        }
    }

    /**
     * Row count for the table. Tombstones are excluded by default; callers that
     * need to know whether the table is populated at all (the bundled seed
     * guard) pass [includeDeleted] = true, because a table that holds only
     * deleted events is still already populated and re-seeding it would
     * resurrect events that were deleted after the snapshot was built.
     */
    fun selectCount(includeDeleted: Boolean = false): Long {
        val where = if (includeDeleted) "" else " WHERE $DELETED_AT IS NULL"
        conn.prepare("SELECT count(*) FROM $TABLE$where;").use {
            it.step()
            return it.getLong(0)
        }
    }
}
