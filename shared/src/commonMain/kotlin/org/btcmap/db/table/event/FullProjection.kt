package org.btcmap.db.table.event

import androidx.sqlite.SQLiteStatement
import io.ktor.http.Url
import kotlin.time.Instant
import org.btcmap.db.getHttpUrlOrNull
import org.btcmap.db.getInstant
import org.btcmap.db.getInstantOrNull
import org.btcmap.db.getTextOrNull

typealias Event = FullProjection

data class FullProjection(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: Url?,
    val startsAt: Instant,
    val endsAt: Instant?,
    // The optional OpenStreetMap place link, in `<type>/<id>` form.
    val placeOsmId: String? = null,
    // Defaults to the epoch for callers that build an event for display only.
    // Sync writes the server's value; selectMaxUpdatedAt reads it back as the
    // delta cursor, and an epoch value simply means "sync from the beginning".
    val updatedAt: Instant = Instant.fromEpochSeconds(0),
    val deletedAt: Instant? = null,
) {
    companion object {
        const val COLUMNS = "$ID, $LAT, $LON, $NAME, $WEBSITE, $STARTS_AT, $ENDS_AT, $PLACE_OSM_ID, $UPDATED_AT, $DELETED_AT"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                lat = stmt.getDouble(1),
                lon = stmt.getDouble(2),
                name = stmt.getText(3),
                website = stmt.getHttpUrlOrNull(4),
                startsAt = stmt.getInstant(5),
                endsAt = stmt.getInstantOrNull(6),
                placeOsmId = stmt.getTextOrNull(7),
                updatedAt = stmt.getInstant(8),
                deletedAt = stmt.getInstantOrNull(9),
            )
        }
    }
}
