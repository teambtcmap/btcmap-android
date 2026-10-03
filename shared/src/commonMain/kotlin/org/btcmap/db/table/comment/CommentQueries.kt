package org.btcmap.db.table.comment

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindInstant
import org.btcmap.db.bindInstantOrNull
import org.btcmap.db.getInstantOrNull
import kotlin.time.Instant
import kotlin.use

class CommentQueries(private val conn: SQLiteConnection) {
    suspend fun insert(rows: List<Comment>) {
        if (rows.isEmpty()) return

        // OR REPLACE, not a plain INSERT: a comment can be returned by the delta
        // sync more than once (the server bumps updated_at when deleted_at
        // changes), and a plain insert would abort the whole sync transaction
        // with a primary-key conflict.
        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE ($ID, $PLACE_ID, $COMMENT, $CREATED_AT, $UPDATED_AT, $DELETED_AT)
            VALUES (?1, ?2, ?3, ?4, ?5, ?6);
            """
        ).use {
            rows.forEach { row ->
                it.bindLong(1, row.id)
                it.bindLong(2, row.placeId)
                it.bindText(3, row.comment)
                it.bindInstant(4, row.createdAt)
                it.bindInstant(5, row.updatedAt)
                it.bindInstantOrNull(6, row.deletedAt)
                it.step()
                it.reset()
            }
        }
    }

    /**
     * Visible comments for a place, newest first.
     *
     * [limit] caps the read for a caller that only shows a few (the place
     * preview); null reads them all (the comments screen).
     */
    suspend fun selectByPlaceId(placeId: Long, limit: Long? = null): List<Comment> {
        val limitClause = if (limit == null) "" else "LIMIT ?2"
        conn.prepare(
            """
            SELECT ${FullProjection.COLUMNS}
            FROM $TABLE
            WHERE $PLACE_ID = ?1
                AND $DELETED_AT IS NULL
            ORDER BY julianday($CREATED_AT) DESC, $ID DESC
            $limitClause;
            """
        ).use {
            it.bindLong(1, placeId)
            if (limit != null) it.bindLong(2, limit)
            val rows = mutableListOf<Comment>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    /**
     * Visible comment count for a place. The place preview reads only the
     * newest few, so it needs the total separately to label the button.
     */
    suspend fun selectCountByPlaceId(placeId: Long): Long {
        conn.prepare(
            """
            SELECT count(*) FROM $TABLE
            WHERE $PLACE_ID = ?1 AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindLong(1, placeId)
            it.step()
            return it.getLong(0)
        }
    }

    suspend fun selectMaxUpdatedAt(): Instant? {
        // julianday, not plain max(): timestamps are stored as text and
        // Instant.toString() is not fixed-width (it drops a zero second
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
            return it.getInstantOrNull(0)
        }
    }

    /**
     * Row count for the table. Tombstones are excluded by default; callers that
     * need to know whether the table is populated at all (the bundled seed
     * guard) pass [includeDeleted] = true, because a table that holds only
     * deleted comments is still already populated and re-seeding it would
     * resurrect comments that were deleted after the snapshot was built.
     */
    suspend fun selectCount(includeDeleted: Boolean = false): Long {
        val where = if (includeDeleted) "" else " WHERE $DELETED_AT IS NULL"
        conn.prepare("SELECT count(*) FROM $TABLE$where;").use {
            it.step()
            return it.getLong(0)
        }
    }
}