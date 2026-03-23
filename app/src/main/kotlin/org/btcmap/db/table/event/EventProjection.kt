package org.btcmap.db.table.event

import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.ZonedDateTime

typealias Event = EventProjectionFull

data class EventProjectionFull(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: HttpUrl,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
) {
    companion object {
        val columns: String
            get() {
                return EventSchema.Columns.entries.joinToString(",") { it.sqlName }
            }

        fun fromStatement(stmt: SQLiteStatement): EventProjectionFull {
            return EventProjectionFull(
                id = stmt.getLong(0),
                lat = stmt.getDouble(1),
                lon = stmt.getDouble(2),
                name = stmt.getText(3),
                website = stmt.getText(4).toHttpUrl(),
                startsAt = ZonedDateTime.parse(stmt.getText(5)),
                endsAt = if (stmt.isNull(6)) null else ZonedDateTime.parse(stmt.getText(6)),
            )
        }
    }
}