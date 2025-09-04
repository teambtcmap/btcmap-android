package db.table.event

import android.database.Cursor
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

        fun fromCursor(cursor: Cursor): EventProjectionFull {
            return EventProjectionFull(
                id = cursor.getLong(0),
                lat = cursor.getDouble(1),
                lon = cursor.getDouble(2),
                name = cursor.getString(3),
                website = cursor.getString(4).toHttpUrl(),
                startsAt = ZonedDateTime.parse(cursor.getString(5)),
                endsAt = if (cursor.isNull(6)) null else ZonedDateTime.parse(cursor.getString(6)),
            )
        }
    }
}