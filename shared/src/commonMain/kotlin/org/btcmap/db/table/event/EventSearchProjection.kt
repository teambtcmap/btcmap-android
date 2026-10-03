package org.btcmap.db.table.event

import androidx.sqlite.SQLiteStatement
import org.btcmap.db.getInstant
import kotlin.time.Instant

typealias SearchEvent = EventSearchProjection

/**
 * The fields an event search needs to rank and render a result: the name to
 * match and show, the location to measure, the id to open, and the start time
 * used to drop events that have already happened. Reading only these instead of
 * [FullProjection] keeps a broad query from materializing every event column.
 */
data class EventSearchProjection(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val startsAt: Instant,
) {
    companion object {
        const val COLUMNS = "$ID, $LAT, $LON, $NAME, $STARTS_AT"

        fun fromStatement(stmt: SQLiteStatement): EventSearchProjection {
            return EventSearchProjection(
                id = stmt.getLong(0),
                lat = stmt.getDouble(1),
                lon = stmt.getDouble(2),
                name = stmt.getText(3),
                startsAt = stmt.getInstant(4),
            )
        }
    }
}
