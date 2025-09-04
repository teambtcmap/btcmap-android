package db.table.comment

import android.database.Cursor
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

        fun fromCursor(cursor: Cursor): CommentProjectionFull {
            return CommentProjectionFull(
                id = cursor.getLong(0),
                placeId = cursor.getLong(1),
                comment = cursor.getString(2),
                createdAt = ZonedDateTime.parse(cursor.getString(3)),
                updatedAt = ZonedDateTime.parse(cursor.getString(4)),
            )
        }
    }
}