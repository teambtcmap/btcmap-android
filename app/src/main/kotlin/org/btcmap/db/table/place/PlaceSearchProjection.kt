package org.btcmap.db.table.place

import androidx.sqlite.SQLiteStatement
import com.google.gson.JsonObject
import org.btcmap.db.getJsonObjectOrNull
import org.btcmap.db.getTextOrNull
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

typealias SearchPlace = PlaceSearchProjection

/**
 * The fields a place search needs to rank and render a result: the names to
 * match and show, the location and icon to render, and the boost used for
 * promotion. Reading only these instead of [FullProjection] keeps a broad query
 * from materializing every column of every matching row.
 */
data class PlaceSearchProjection(
    val id: Long,
    val name: String?,
    val localizedName: JsonObject?,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val boostedUntil: ZonedDateTime?,
) {
    companion object {
        const val COLUMNS = "$ID, $NAME, $LOCALIZED_NAME, $LAT, $LON, $ICON, $BOOSTED_UNTIL"

        fun fromStatement(stmt: SQLiteStatement): PlaceSearchProjection {
            return PlaceSearchProjection(
                id = stmt.getLong(0),
                name = stmt.getTextOrNull(1),
                localizedName = stmt.getJsonObjectOrNull(2),
                lat = stmt.getDouble(3),
                lon = stmt.getDouble(4),
                icon = stmt.getText(5),
                boostedUntil = stmt.getZonedDateTimeOrNull(6),
            )
        }
    }
}
