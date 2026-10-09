package org.btcmap.db.table.note

import androidx.sqlite.SQLiteConnection
import kotlin.use
import org.btcmap.db.bindInstant
import org.btcmap.db.getInstant

/**
 * Reads and writes the signed-in user's personal notes, newest first.
 *
 * The table is a pure cache of one owner's notes: the sync rewrites it whole
 * (see [deleteAll] + [insert]), so there is no cursor, index or tombstone here.
 */
class NoteQueries(private val conn: SQLiteConnection) {

    suspend fun insert(rows: List<Note>) {
        if (rows.isEmpty()) return

        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE (
                $ID, $LAT, $LON, $TEXT, $ICON, $IS_PUBLIC, $CREATED_AT, $UPDATED_AT
            )
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8);
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
                it.step()
                it.reset()
            }
        }
    }

    /** Every cached note, newest first (the order the endpoint returns them in). */
    suspend fun selectAll(): List<Note> {
        conn.prepare(
            """
            SELECT $ID, $LAT, $LON, $TEXT, $ICON, $IS_PUBLIC, $CREATED_AT, $UPDATED_AT
            FROM $TABLE
            ORDER BY julianday($CREATED_AT) DESC, $ID DESC;
            """
        ).use {
            val rows = mutableListOf<Note>()
            while (it.step()) {
                rows.add(
                    Note(
                        id = it.getLong(0),
                        lat = it.getDouble(1),
                        lon = it.getDouble(2),
                        text = it.getText(3),
                        icon = it.getText(4),
                        public = it.getLong(5) != 0L,
                        createdAt = it.getInstant(6),
                        updatedAt = it.getInstant(7),
                    )
                )
            }
            return rows
        }
    }

    suspend fun deleteAll() {
        conn.prepare("DELETE FROM $TABLE;").use { it.step() }
    }

    /** The cached note with [id], or null when it is not cached. */
    suspend fun selectById(id: Long): Note? {
        conn.prepare(
            """
            SELECT $ID, $LAT, $LON, $TEXT, $ICON, $IS_PUBLIC, $CREATED_AT, $UPDATED_AT
            FROM $TABLE
            WHERE $ID = ?1;
            """
        ).use {
            it.bindLong(1, id)
            if (!it.step()) return null
            return Note(
                id = it.getLong(0),
                lat = it.getDouble(1),
                lon = it.getDouble(2),
                text = it.getText(3),
                icon = it.getText(4),
                public = it.getLong(5) != 0L,
                createdAt = it.getInstant(6),
                updatedAt = it.getInstant(7),
            )
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

    /** Number of cached notes. */
    suspend fun selectCount(): Long {
        conn.prepare("SELECT count(*) FROM $TABLE;").use {
            it.step()
            return it.getLong(0)
        }
    }
}
