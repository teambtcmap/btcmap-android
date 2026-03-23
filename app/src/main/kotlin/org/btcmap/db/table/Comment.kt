package org.btcmap.db.table

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import java.time.ZonedDateTime

object CommentSchema {
    const val NAME = "comment"

    override fun toString(): String {
        return """
            CREATE TABLE $NAME (
                ${Columns.Id} INTEGER PRIMARY KEY NOT NULL,
                ${Columns.PlaceId} INTEGER NOT NULL,
                ${Columns.Comment} TEXT NOT NULL,
                ${Columns.CreatedAt} TEXT NOT NULL,
                ${Columns.UpdatedAt} TEXT NOT NULL
            )
        """
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
                rows.add(CommentProjectionFull.fromStatement(stmt))
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
            conn.prepare("DELETE FROM ${CommentSchema.NAME} WHERE ${CommentSchema.Columns.Id.sqlName} = ?1")
        stmt.bindLong(1, id)
        stmt.step()
    }
}