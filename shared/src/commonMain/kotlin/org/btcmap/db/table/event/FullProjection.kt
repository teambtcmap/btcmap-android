package org.btcmap.db.table.event

import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import org.btcmap.db.getHttpUrlOrNull
import org.btcmap.db.getZonedDateTime
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

typealias Event = FullProjection

data class FullProjection(
    val id: Long,
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
        const val COLUMNS = "$ID, $LAT, $LON, $NAME, $WEBSITE, $STARTS_AT, $ENDS_AT, $UPDATED_AT, $DELETED_AT"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                lat = stmt.getDouble(1),
                lon = stmt.getDouble(2),
                name = stmt.getText(3),
                website = stmt.getHttpUrlOrNull(4),
                startsAt = stmt.getZonedDateTime(5),
                endsAt = stmt.getZonedDateTimeOrNull(6),
                updatedAt = stmt.getZonedDateTime(7),
                deletedAt = stmt.getZonedDateTimeOrNull(8),
            )
        }
    }
}
