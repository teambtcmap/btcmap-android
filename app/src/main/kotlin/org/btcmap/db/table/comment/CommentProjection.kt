package org.btcmap.db.table.comment

import androidx.sqlite.SQLiteStatement
import java.time.ZonedDateTime

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