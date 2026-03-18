package org.btcmap.db.table.comment

import android.database.sqlite.SQLiteDatabase
import org.btcmap.db.table.comment.CommentProjectionFull.Companion.fromCursor
import java.time.ZonedDateTime

class CommentQueries(private val db: SQLiteDatabase) {

    fun insert(
        rows: List<Comment>,
    ) {
        val sql = """
            INSERT INTO ${CommentSchema.NAME} (${Comment.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5)
        """

        val stmt = db.compileStatement(sql)

        stmt.use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindLong(2, row.placeId)
                stmt.bindString(3, row.comment)
                stmt.bindString(4, row.createdAt.toString())
                stmt.bindString(5, row.updatedAt.toString())
                stmt.executeInsert()
            }
        }
    }

    fun selectByPlaceId(placeId: Long): List<Comment> {
        val cursor = db.rawQuery(
            """
                SELECT ${Comment.columns}
                FROM ${CommentSchema.NAME}
                WHERE ${CommentSchema.Columns.PlaceId} = ?1
                ORDER BY ${CommentSchema.Columns.CreatedAt} DESC
            """,
            arrayOf(placeId.toString())
        )

        cursor.use {
            val rows = mutableListOf<Comment>()

            while (cursor.moveToNext()) {
                rows.add(fromCursor(cursor))
            }

            return rows
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        db.rawQuery("SELECT max(updated_at) FROM ${CommentSchema.NAME}", null).use {
            it.moveToFirst()
            return if (it.isNull(0)) null else ZonedDateTime.parse(it.getString(0))
        }
    }

    fun deleteById(id: Long): Int {
        return db.delete(
            CommentSchema.NAME,
            "${CommentSchema.Columns.Id.sqlName} = ?1",
            arrayOf(id.toString()),
        )
    }
}