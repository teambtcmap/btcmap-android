package org.btcmap.db.table.comment

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.table.comment.CommentProjectionFull.Companion.fromStatement
import java.time.ZonedDateTime

class CommentQueries(private val conn: SQLiteConnection) {

    fun insert(
        rows: List<Comment>,
    ) {
        val sql = """
            INSERT INTO ${CommentSchema.NAME} (${Comment.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5)
        """

        val stmt = conn.prepare(sql)

        stmt.use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindLong(2, row.placeId)
                stmt.bindText(3, row.comment)
                stmt.bindText(4, row.createdAt.toString())
                stmt.bindText(5, row.updatedAt.toString())
                stmt.step()
            }
        }
    }

    fun selectByPlaceId(placeId: Long): List<Comment> {
        val stmt = conn.prepare(
            """
                SELECT ${Comment.columns}
                FROM ${CommentSchema.NAME}
                WHERE ${CommentSchema.Columns.PlaceId} = ?1
                ORDER BY ${CommentSchema.Columns.CreatedAt} DESC
            """
        )
        stmt.bindLong(1, placeId)

        stmt.use {
            val rows = mutableListOf<Comment>()

            while (stmt.step()) {
                rows.add(fromStatement(stmt))
            }

            return rows
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        conn.prepare("SELECT max(updated_at) FROM ${CommentSchema.NAME}").use {
            it.step()
            return if (it.isNull(0)) null else ZonedDateTime.parse(it.getText(0))
        }
    }

    fun deleteById(id: Long) {
        val stmt =
            conn.prepare("DELETE FROM ${CommentSchema.NAME} WHERE ${CommentSchema.Columns.Id.sqlName} = ?")
        stmt.bindLong(1, id)
        stmt.step()
        stmt.close()
    }
}