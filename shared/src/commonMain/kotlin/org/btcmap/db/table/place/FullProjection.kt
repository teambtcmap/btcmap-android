package org.btcmap.db.table.place

import androidx.sqlite.SQLiteStatement
import io.ktor.http.Url
import kotlinx.serialization.json.JsonObject
import org.btcmap.db.getHttpUrlOrNull
import org.btcmap.db.getJsonObjectOrNull
import org.btcmap.db.getLongOrNull
import org.btcmap.db.getTextOrNull
import org.btcmap.db.getInstant
import org.btcmap.db.getInstantOrNull
import kotlin.time.Instant

typealias Place = FullProjection

data class FullProjection(
    val id: Long,
    val updatedAt: Instant,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String?,
    val localizedName: JsonObject?,
    val verifiedAt: Instant?,
    val address: String?,
    val openingHours: String?,
    val phone: String?,
    val website: Url?,
    val email: String?,
    val twitter: Url?,
    val facebook: Url?,
    val instagram: Url?,
    val line: Url?,
    val requiredAppUrl: Url?,
    val boostedUntil: Instant?,
    val comments: Long?,
    val telegram: Url?,
    val osmId: String?,
    val deletedAt: Instant? = null,
) {
    companion object {
        const val COLUMNS = "$ID, $UPDATED_AT, $LAT, $LON, $ICON, $NAME, $LOCALIZED_NAME, $VERIFIED_AT, $ADDRESS, $OPENING_HOURS, $PHONE, $WEBSITE, $EMAIL, $TWITTER, $FACEBOOK, $INSTAGRAM, $LINE, $REQUIRED_APP_URL, $BOOSTED_UNTIL, $COMMENTS, $TELEGRAM, $OSM_ID, $DELETED_AT"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                updatedAt = stmt.getInstant(1),
                lat = stmt.getDouble(2),
                lon = stmt.getDouble(3),
                icon = stmt.getText(4),
                name = stmt.getTextOrNull(5),
                localizedName = stmt.getJsonObjectOrNull(6),
                verifiedAt = stmt.getInstantOrNull(7),
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
                boostedUntil = stmt.getInstantOrNull(18),
                comments = stmt.getLongOrNull(19),
                telegram = stmt.getHttpUrlOrNull(20),
                osmId = stmt.getTextOrNull(21),
                deletedAt = stmt.getInstantOrNull(22),
            )
        }
    }
}