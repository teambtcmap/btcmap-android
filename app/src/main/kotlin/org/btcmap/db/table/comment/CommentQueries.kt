package org.btcmap.db.table.comment

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindZonedDateTime
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime
import kotlin.use

class CommentQueries(private val conn: SQLiteConnection) {
    fun insert(rows: List<Comment>) {
        // OR REPLACE, not a plain INSERT: a comment can be returned by the delta
        // sync more than once (the server bumps updated_at when deleted_at
        // changes), and a plain insert would abort the whole sync transaction
        // with a primary-key conflict.
        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE ($ID, $PLACE_ID, $COMMENT, $CREATED_AT, $UPDATED_AT)
            VALUES (?1, ?2, ?3, ?4, ?5);
            """
        ).use {
            rows.forEach { row ->
                it.bindLong(1, row.id)
                it.bindLong(2, row.placeId)
                it.bindText(3, row.comment)
                it.bindZonedDateTime(4, row.createdAt)
                it.bindZonedDateTime(5, row.updatedAt)
                it.step()
                it.reset()
            }
        }
    }

    fun selectByPlaceId(placeId: Long): List<Comment> {
        conn.prepare(
            """
            SELECT ${FullProjection.COLUMNS}
            FROM $TABLE
            WHERE $PLACE_ID = ?1
            ORDER BY julianday($CREATED_AT) DESC, $ID DESC;
            """
        ).use {
            it.bindLong(1, placeId)
            val rows = mutableListOf<Comment>()
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

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM $TABLE WHERE $ID = ?1;")
            .use {
                it.bindLong(1, id)
                it.step()
            }
    }
}