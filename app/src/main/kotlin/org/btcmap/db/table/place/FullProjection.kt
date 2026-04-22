package org.btcmap.db.table.place

import androidx.sqlite.SQLiteStatement
import com.google.gson.JsonObject
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
    val bundled: Boolean,
    val updatedAt: ZonedDateTime,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String?,
    val localizedName: JsonObject?,
    val verifiedAt: ZonedDateTime?,
    val address: String?,
    val openingHours: String?,
    val localizedOpeningHours: JsonObject?,
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
        const val COLUMNS = "$ID, $BUNDLED, $UPDATED_AT, $LAT, $LON, $ICON, $NAME, $LOCALIZED_NAME, $VERIFIED_AT, $ADDRESS, $OPENING_HOURS, $LOCALIZED_OPENING_HOURS, $PHONE, $WEBSITE, $EMAIL, $TWITTER, $FACEBOOK, $INSTAGRAM, $LINE, $REQUIRED_APP_URL, $BOOSTED_UNTIL, $COMMENTS, $TELEGRAM"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                bundled = stmt.getLong(1) != 0L,
                updatedAt = stmt.getZonedDateTime(2),
                lat = stmt.getDouble(3),
                lon = stmt.getDouble(4),
                icon = stmt.getText(5),
                name = stmt.getTextOrNull(6),
                localizedName = stmt.getJsonObjectOrNull(7),
                verifiedAt = stmt.getZonedDateTimeOrNull(8),
                address = stmt.getTextOrNull(9),
                openingHours = stmt.getTextOrNull(10),
                localizedOpeningHours = stmt.getJsonObjectOrNull(11),
                phone = stmt.getTextOrNull(12),
                website = stmt.getHttpUrlOrNull(13),
                email = stmt.getTextOrNull(14),
                twitter = stmt.getHttpUrlOrNull(15),
                facebook = stmt.getHttpUrlOrNull(16),
                instagram = stmt.getHttpUrlOrNull(17),
                line = stmt.getHttpUrlOrNull(18),
                requiredAppUrl = stmt.getHttpUrlOrNull(19),
                boostedUntil = stmt.getZonedDateTimeOrNull(20),
                comments = stmt.getLongOrNull(21),
                telegram = stmt.getHttpUrlOrNull(22),
            )
        }
    }
}