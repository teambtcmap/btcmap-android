package org.btcmap.db.table.place

import androidx.sqlite.SQLiteStatement
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import org.btcmap.db.getHttpUrlOrNull
import org.btcmap.db.getJsonObjectOrNull
import org.btcmap.db.getLongOrNull
import org.btcmap.db.getTextOrNull
import org.btcmap.db.getZonedDateTime
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

typealias Place = FullProjection

data class FullProjection(
    val id: Long,
    val updatedAt: ZonedDateTime,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String?,
    val localizedName: JsonObject?,
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
    val osmId: String?,
    val deletedAt: ZonedDateTime? = null,
) {
    companion object {
        const val COLUMNS = "$ID, $UPDATED_AT, $LAT, $LON, $ICON, $NAME, $LOCALIZED_NAME, $VERIFIED_AT, $ADDRESS, $OPENING_HOURS, $PHONE, $WEBSITE, $EMAIL, $TWITTER, $FACEBOOK, $INSTAGRAM, $LINE, $REQUIRED_APP_URL, $BOOSTED_UNTIL, $COMMENTS, $TELEGRAM, $OSM_ID, $DELETED_AT"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                updatedAt = stmt.getZonedDateTime(1),
                lat = stmt.getDouble(2),
                lon = stmt.getDouble(3),
                icon = stmt.getText(4),
                name = stmt.getTextOrNull(5),
                localizedName = stmt.getJsonObjectOrNull(6),
                verifiedAt = stmt.getZonedDateTimeOrNull(7),
                address = stmt.getTextOrNull(8),
                openingHours = stmt.getTextOrNull(9),
                phone = stmt.getTextOrNull(10),
                website = stmt.getHttpUrlOrNull(11),
                email = stmt.getTextOrNull(12),
                twitter = stmt.getHttpUrlOrNull(13),
                facebook = stmt.getHttpUrlOrNull(14),
                instagram = stmt.getHttpUrlOrNull(15),
                line = stmt.getHttpUrlOrNull(16),
                requiredAppUrl = stmt.getHttpUrlOrNull(17),
                boostedUntil = stmt.getZonedDateTimeOrNull(18),
                comments = stmt.getLongOrNull(19),
                telegram = stmt.getHttpUrlOrNull(20),
                osmId = stmt.getTextOrNull(21),
                deletedAt = stmt.getZonedDateTimeOrNull(22),
            )
        }
    }
}