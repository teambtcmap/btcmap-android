package org.btcmap.db.table.comment

import androidx.sqlite.SQLiteStatement
import org.btcmap.db.getInstant
import org.btcmap.db.getInstantOrNull
import kotlin.time.Instant

typealias Comment = FullProjection

data class FullProjection(
    val id: Long,
    val placeId: Long,
    val comment: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
) {
    companion object {
        const val COLUMNS = "$ID, $PLACE_ID, $COMMENT, $CREATED_AT, $UPDATED_AT, $DELETED_AT"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                placeId = stmt.getLong(1),
                comment = stmt.getText(2),
                createdAt = stmt.getInstant(3),
                updatedAt = stmt.getInstant(4),
                deletedAt = stmt.getInstantOrNull(5),
            )
        }
    }
}