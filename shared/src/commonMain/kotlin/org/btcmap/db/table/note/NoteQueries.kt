package org.btcmap.db.table.note

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlin.time.Instant
import kotlin.use
import org.btcmap.db.bindInstant
import org.btcmap.db.bindInstantOrNull
import org.btcmap.db.getInstant
import org.btcmap.db.getInstantOrNull

/**
 * Reads and writes the signed-in user's personal notes, newest first.
 *
 * The table caches one owner's notes and mirrors the other tables' delta sync:
 * a soft-deleted note is kept as a tombstone so `max(updated_at)` advances past
 * the deletion, and reads exclude it.
 */
class NoteQueries(private val conn: SQLiteConnection) {

    suspend fun insert(rows: List<Note>) {
        if (rows.isEmpty()) return

        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE (
                $ID, $LAT, $LON, $TEXT, $ICON, $IS_PUBLIC, $CREATED_AT, $UPDATED_AT, $DELETED_AT
            )
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9);
            """
        ).use {
            rows.forEach { row ->
                it.bindLong(1, row.id)
                it.bindDouble(2, row.lat)
                it.bindDouble(3, row.lon)
                it.bindText(4, row.text)
                it.bindText(5, row.icon)
                it.bindLong(6, if (row.public) 1L else 0L)
                it.bindInstant(7, row.createdAt)
                it.bindInstant(8, row.updatedAt)
                it.bindInstantOrNull(9, row.deletedAt)
                it.step()
                it.reset()
            }
        }
    }

    /**
     * Every live cached note, newest first (the order the endpoint returns them
     * in). Soft-deleted notes are kept as tombstones for the sync cursor and are
     * excluded here.
     */
    suspend fun selectAll(): List<Note> {
        conn.prepare(
            """
            SELECT $ID, $LAT, $LON, $TEXT, $ICON, $IS_PUBLIC, $CREATED_AT, $UPDATED_AT, $DELETED_AT
            FROM $TABLE
            WHERE $DELETED_AT IS NULL
            ORDER BY julianday($CREATED_AT) DESC, $ID DESC;
            """
        ).use {
            val rows = mutableListOf<Note>()
            while (it.step()) {
                rows.add(it.toNote())
            }
            return rows
        }
    }

    /** The newest `updated_at` in the table, tombstone included: the sync cursor. */
    suspend fun selectMaxUpdatedAt(): Instant? {
        conn.prepare(
            """
            SELECT $UPDATED_AT
            FROM $TABLE
            ORDER BY julianday($UPDATED_AT) DESC
            LIMIT 1;
            """
        ).use {
            if (!it.step()) return null
            return it.getInstantOrNull(0)
        }
    }

    suspend fun deleteAll() {
        conn.prepare("DELETE FROM $TABLE;").use { it.step() }
    }

    /** The live cached note with [id], or null when it is not cached or deleted. */
    suspend fun selectById(id: Long): Note? {
        conn.prepare(
            """
            SELECT $ID, $LAT, $LON, $TEXT, $ICON, $IS_PUBLIC, $CREATED_AT, $UPDATED_AT, $DELETED_AT
            FROM $TABLE
            WHERE $ID = ?1 AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindLong(1, id)
            if (!it.step()) return null
            return it.toNote()
        }
    }

    suspend fun updateVisibility(id: Long, public: Boolean) {
        conn.prepare("UPDATE $TABLE SET $IS_PUBLIC = ?2 WHERE $ID = ?1;").use {
            it.bindLong(1, id)
            it.bindLong(2, if (public) 1L else 0L)
            it.step()
        }
    }

    /** Replaces a cached note's body with [text]. */
    suspend fun updateText(id: Long, text: String) {
        conn.prepare("UPDATE $TABLE SET $TEXT = ?2 WHERE $ID = ?1;").use {
            it.bindLong(1, id)
            it.bindText(2, text)
            it.step()
        }
    }

    /** Replaces a cached note's icon with [icon]. */
    suspend fun updateIcon(id: Long, icon: String) {
        conn.prepare("UPDATE $TABLE SET $ICON = ?2 WHERE $ID = ?1;").use {
            it.bindLong(1, id)
            it.bindText(2, icon)
            it.step()
        }
    }

    suspend fun delete(id: Long) {
        conn.prepare("DELETE FROM $TABLE WHERE $ID = ?1;").use {
            it.bindLong(1, id)
            it.step()
        }
    }

    /**
     * Row count for the table. Tombstones are excluded by default; pass
     * [includeDeleted] = true to count every row.
     */
    suspend fun selectCount(includeDeleted: Boolean = false): Long {
        val where = if (includeDeleted) "" else " WHERE $DELETED_AT IS NULL"
        conn.prepare("SELECT count(*) FROM $TABLE$where;").use {
            it.step()
            return it.getLong(0)
        }
    }
}

/** Reads one note row, tombstone included, from the current statement position. */
private fun SQLiteStatement.toNote(): Note = Note(
    id = getLong(0),
    lat = getDouble(1),
    lon = getDouble(2),
    text = getText(3),
    icon = getText(4),
    public = getLong(5) != 0L,
    createdAt = getInstant(6),
    updatedAt = getInstant(7),
    deletedAt = getInstantOrNull(8),
)
