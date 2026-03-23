package org.btcmap.db.table

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

object CommentSchema {
    const val TABLE_NAME = "comment"

    override fun toString(): String {
        return """
            CREATE TABLE $TABLE_NAME (
                ${Columns.Id} INTEGER PRIMARY KEY NOT NULL,
                ${Columns.PlaceId} INTEGER NOT NULL,
                ${Columns.Comment} TEXT NOT NULL,
                ${Columns.CreatedAt} TEXT NOT NULL,
                ${Columns.UpdatedAt} TEXT NOT NULL
            );
        """.trimIndent()
    }

    enum class Columns(val sqlName: String) {
        Id("id"),
        PlaceId("place_id"),
        Comment("comment"),
        CreatedAt("created_at"),
        UpdatedAt("updated_at");

        override fun toString() = sqlName
    }
}

typealias Comment = CommentProjectionFull

data class CommentProjectionFull(
    val id: Long,
    val placeId: Long,
    val comment: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        val columns: String
            get() {
                return CommentSchema.Columns.entries.joinToString(",") { it.sqlName }
            }

        fun fromStatement(stmt: SQLiteStatement): CommentProjectionFull {
            return CommentProjectionFull(
                id = stmt.getLong(0),
                placeId = stmt.getLong(1),
                comment = stmt.getText(2),
                createdAt = ZonedDateTime.parse(stmt.getText(3)),
                updatedAt = ZonedDateTime.parse(stmt.getText(4)),
            )
        }
    }
}

class CommentQueries(private val conn: SQLiteConnection) {
    fun insert(rows: List<Comment>) {
        val sql = """
            INSERT INTO ${CommentSchema.TABLE_NAME} (${Comment.columns})
            VALUES (?1, ?2, ?3, ?4, ?5);
        """

        val stmt = conn.prepare(sql)

        stmt.use {
            rows.forEach { row ->
                it.bindLong(1, row.id)
                it.bindLong(2, row.placeId)
                it.bindText(3, row.comment)
                it.bindText(4, row.createdAt.toString())
                it.bindText(5, row.updatedAt.toString())
                it.step()
                it.reset()
            }
        }
    }

    fun selectByPlaceId(placeId: Long): List<Comment> {
        val stmt = conn.prepare(
            """
                SELECT ${Comment.columns}
                FROM ${CommentSchema.TABLE_NAME}
                WHERE ${CommentSchema.Columns.PlaceId} = ?1
                ORDER BY ${CommentSchema.Columns.CreatedAt} DESC;
            """
        )
        stmt.bindLong(1, placeId)

        stmt.use {
            val rows = mutableListOf<Comment>()

            while (it.step()) {
                rows.add(CommentProjectionFull.fromStatement(it))
            }

            return rows
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        conn.prepare("SELECT max(updated_at) FROM ${CommentSchema.TABLE_NAME};").use {
            it.step()
            return it.getZonedDateTimeOrNull(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM ${CommentSchema.TABLE_NAME} WHERE ${CommentSchema.Columns.Id.sqlName} = ?1;")
            .use {
                it.bindLong(1, id)
                it.step()
            }
    }
}