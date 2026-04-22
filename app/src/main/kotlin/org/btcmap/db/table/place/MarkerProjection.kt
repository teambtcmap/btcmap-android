package org.btcmap.db.table.place

import androidx.sqlite.SQLiteStatement
import org.btcmap.db.getLongOrNull
import org.btcmap.db.getTextOrNull

typealias Marker = MarkerProjection

data class MarkerProjection(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val boostedUntil: String?,
    val requiredAppUrl: String?,
    val comments: Long,
) {
    companion object {
        const val COLUMNS = "$ID, $LAT, $LON, $ICON, $BOOSTED_UNTIL, $REQUIRED_APP_URL, $COMMENTS"

        fun fromStatement(stmt: SQLiteStatement): MarkerProjection {
            return MarkerProjection(
                id = stmt.getLong(0),
                lat = stmt.getDouble(1),
                lon = stmt.getDouble(2),
                icon = stmt.getText(3),
                boostedUntil = stmt.getTextOrNull(4),
                requiredAppUrl = stmt.getTextOrNull(5),
                comments = stmt.getLongOrNull(6) ?: 0,
            )
        }
    }
}