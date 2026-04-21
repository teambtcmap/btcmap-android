package org.btcmap.db.table.event

import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.db.getLongOrNull
import org.btcmap.db.getTextOrNull
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

typealias Event = FullProjection

data class FullProjection(
    val id: Long,
    val areaId: Long?,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: HttpUrl,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
    val cronSchedule: String?,
) {
    companion object {
        const val COLUMNS = "$ID, $AREA_ID, $LAT, $LON, $NAME, $WEBSITE, $STARTS_AT, $ENDS_AT, $CRON_SCHEDULE"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                areaId = stmt.getLongOrNull(1),
                lat = stmt.getDouble(2),
                lon = stmt.getDouble(3),
                name = stmt.getText(4),
                website = stmt.getText(5).toHttpUrl(),
                startsAt = ZonedDateTime.parse(stmt.getText(6)),
                endsAt = stmt.getZonedDateTimeOrNull(7),
                cronSchedule = stmt.getTextOrNull(8),
            )
        }
    }
}