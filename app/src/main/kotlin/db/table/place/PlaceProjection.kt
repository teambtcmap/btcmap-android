package db.table.place

import android.database.Cursor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.ZonedDateTime

typealias Place = PlaceProjectionFull

data class PlaceProjectionFull(
    val id: Long,
    val bundled: Boolean,
    val updatedAt: ZonedDateTime,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String?,
    val verifiedAt: ZonedDateTime?,
    val address: String?,
    val openingHours: String?,
    val phone: String?,
    val website: HttpUrl?,
    val email: String?,
    val twitter: HttpUrl?,
    val facebook: HttpUrl?,
    val instagram: HttpUrl?,
    val line: HttpUrl?,
    val requiredAppUrl: HttpUrl?,
    val boostedUntil: ZonedDateTime?,
    val comments: Long?,
    val telegram: HttpUrl?,
) {
    companion object {
        val columns: String
            get() {
                return PlaceSchema.Columns.entries.joinToString(",") { it.sqlName }
            }

        fun fromCursor(cursor: Cursor): PlaceProjectionFull {
            return PlaceProjectionFull(
                id = cursor.getLong(0),
                bundled = cursor.getInt(1) != 0,
                updatedAt = ZonedDateTime.parse(cursor.getString(2)),
                lat = cursor.getDouble(3),
                lon = cursor.getDouble(4),
                icon = cursor.getString(5),
                name = if (cursor.isNull(6)) null else cursor.getString(6),
                verifiedAt = if (cursor.isNull(7)) null else ZonedDateTime.parse(cursor.getString(7)),
                address = if (cursor.isNull(8)) null else cursor.getString(8),
                openingHours = if (cursor.isNull(9)) null else cursor.getString(9),
                phone = if (cursor.isNull(10)) null else cursor.getString(10),
                website = if (cursor.isNull(11)) null else cursor.getString(11).toHttpUrl(),
                email = if (cursor.isNull(12)) null else cursor.getString(12),
                twitter = if (cursor.isNull(13)) null else cursor.getString(13).toHttpUrl(),
                facebook = if (cursor.isNull(14)) null else cursor.getString(14).toHttpUrl(),
                instagram = if (cursor.isNull(15)) null else cursor.getString(15).toHttpUrl(),
                line = if (cursor.isNull(16)) null else cursor.getString(16).toHttpUrl(),
                requiredAppUrl = if (cursor.isNull(17)) null else cursor.getString(17).toHttpUrl(),
                boostedUntil = if (cursor.isNull(18)) null else ZonedDateTime.parse(
                    cursor.getString(
                        18
                    )
                ),
                comments = if (cursor.isNull(19)) null else cursor.getLong(19),
                telegram = if (cursor.isNull(20)) null else cursor.getString(20).toHttpUrl(),
            )
        }
    }
}

typealias Cluster = PlaceProjectionCluster

data class PlaceProjectionCluster(
    val count: Long,
    val id: Long,
    val lat: Double,
    val lon: Double,
    val iconId: String,
    val boostExpires: ZonedDateTime?,
    val requiresCompanionApp: Boolean,
    val comments: Long,
)