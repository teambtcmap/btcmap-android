package org.btcmap.db.table.comment

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindZonedDateTime
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime
import kotlin.use

class CommentQueries(private val conn: SQLiteConnection) {
    fun insert(rows: List<Comment>) {
        conn.prepare(
            """
            INSERT INTO $TABLE ($ID, $PLACE_ID, $COMMENT, $CREATED_AT, $UPDATED_AT)
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
            ORDER BY $CREATED_AT DESC;
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
        conn.prepare("SELECT max($UPDATED_AT) FROM $TABLE;").use {
            it.step()
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