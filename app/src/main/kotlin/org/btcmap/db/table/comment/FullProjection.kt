package org.btcmap.db.table.comment

import androidx.sqlite.SQLiteStatement
import org.btcmap.db.getZonedDateTime
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

typealias Comment = FullProjection

data class FullProjection(
    val id: Long,
    val placeId: Long,
    val comment: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime? = null,
) {
    companion object {
        const val COLUMNS = "$ID, $PLACE_ID, $COMMENT, $CREATED_AT, $UPDATED_AT, $DELETED_AT"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                placeId = stmt.getLong(1),
                comment = stmt.getText(2),
                createdAt = stmt.getZonedDateTime(3),
                updatedAt = stmt.getZonedDateTime(4),
                deletedAt = stmt.getZonedDateTimeOrNull(5),
            )
        }
    }
}