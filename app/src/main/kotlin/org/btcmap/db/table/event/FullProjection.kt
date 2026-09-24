package org.btcmap.db.table.event

import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import org.btcmap.db.getHttpUrlOrNull
import org.btcmap.db.getLongOrNull
import org.btcmap.db.getZonedDateTime
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

typealias Event = FullProjection

data class FullProjection(
    val id: Long,
    // Legacy and effectively always null: the server's v4 event payload does
    // not send `area_id`, so sync writes null and nothing reads this for
    // behaviour. The event-to-area link is resolved geometrically instead (see
    // `isWithin`), which is also why the area screen and map chips never query
    // by area_id. Kept on the schema rather than dropped, because removing the
    // column would force an event-table rebuild migration for no user benefit.
    val areaId: Long?,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: HttpUrl?,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
    // Defaults to the epoch for callers that build an event for display only.
    // Sync writes the server's value; selectMaxUpdatedAt reads it back as the
    // delta cursor, and an epoch value simply means "sync from the beginning".
    val updatedAt: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC),
    val deletedAt: ZonedDateTime? = null,
) {
    companion object {
        const val COLUMNS = "$ID, $AREA_ID, $LAT, $LON, $NAME, $WEBSITE, $STARTS_AT, $ENDS_AT, $UPDATED_AT, $DELETED_AT"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                areaId = stmt.getLongOrNull(1),
                lat = stmt.getDouble(2),
                lon = stmt.getDouble(3),
                name = stmt.getText(4),
                website = stmt.getHttpUrlOrNull(5),
                startsAt = stmt.getZonedDateTime(6),
                endsAt = stmt.getZonedDateTimeOrNull(7),
                updatedAt = stmt.getZonedDateTime(8),
                deletedAt = stmt.getZonedDateTimeOrNull(9),
            )
        }
    }
}
