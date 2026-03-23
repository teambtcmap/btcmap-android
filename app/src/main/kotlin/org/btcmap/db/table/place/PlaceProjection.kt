package org.btcmap.db.table.place

import android.content.Context
import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.R
import org.json.JSONObject
import java.time.ZonedDateTime
import java.util.Locale

typealias Place = PlaceProjectionFull

data class PlaceProjectionFull(
    val id: Long,
    val bundled: Boolean,
    val updatedAt: ZonedDateTime,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String?,
    val localizedName: JSONObject?,
    val verifiedAt: ZonedDateTime?,
    val address: String?,
    val openingHours: String?,
    val localizedOpeningHours: JSONObject?,
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

        fun fromStatement(stmt: SQLiteStatement): PlaceProjectionFull {
            return PlaceProjectionFull(
                id = stmt.getLong(0),
                bundled = stmt.getLong(1) != 0L,
                updatedAt = ZonedDateTime.parse(stmt.getText(2)),
                lat = stmt.getDouble(3),
                lon = stmt.getDouble(4),
                icon = stmt.getText(5),
                name = if (stmt.isNull(6)) null else stmt.getText(6),
                localizedName = if (stmt.isNull(7)) null else JSONObject(stmt.getText(7)),
                verifiedAt = if (stmt.isNull(8)) null else ZonedDateTime.parse(stmt.getText(8)),
                address = if (stmt.isNull(9)) null else stmt.getText(9),
                openingHours = if (stmt.isNull(10)) null else stmt.getText(10),
                localizedOpeningHours = if (stmt.isNull(11)) null else JSONObject(stmt.getText(11)),
                phone = if (stmt.isNull(12)) null else stmt.getText(12),
                website = if (stmt.isNull(13)) null else stmt.getText(13).toHttpUrl(),
                email = if (stmt.isNull(14)) null else stmt.getText(14),
                twitter = if (stmt.isNull(15)) null else stmt.getText(15).toHttpUrl(),
                facebook = if (stmt.isNull(16)) null else stmt.getText(16).toHttpUrl(),
                instagram = if (stmt.isNull(17)) null else stmt.getText(17).toHttpUrl(),
                line = if (stmt.isNull(18)) null else stmt.getText(18).toHttpUrl(),
                requiredAppUrl = if (stmt.isNull(19)) null else stmt.getText(19).toHttpUrl(),
                boostedUntil = if (stmt.isNull(20)) null else ZonedDateTime.parse(stmt.getText(20)),
                comments = if (stmt.isNull(21)) null else stmt.getLong(21),
                telegram = if (stmt.isNull(22)) null else stmt.getText(22).toHttpUrl(),
            )
        }
    }
}

fun Place.getLocalizedName(): String? {
    val locale = Locale.getDefault().language
    return localizedName?.optString(locale) ?: name
}

fun Place.getLocalizedOpeningHours(context: Context): String? {
    val locale = Locale.getDefault().language

    val result = if (localizedOpeningHours != null) {
        val localized = localizedOpeningHours.optString(locale)

        localized.ifBlank {
            val en = localizedOpeningHours.optString("en")

            en.ifBlank {
                openingHours
            }
        }
    } else {
        openingHours
    }

    return result?.translate(context)
}

private fun String.translate(context: Context): String {
    return this
        .replace("Monday", context.getString(R.string.monday), ignoreCase = true)
        .replace("Tuesday", context.getString(R.string.tuesday), ignoreCase = true)
        .replace("Wednesday", context.getString(R.string.wednesday), ignoreCase = true)
        .replace("Thursday", context.getString(R.string.thursday), ignoreCase = true)
        .replace("Friday", context.getString(R.string.friday), ignoreCase = true)
        .replace("Saturday", context.getString(R.string.saturday), ignoreCase = true)
        .replace("Sunday", context.getString(R.string.sunday), ignoreCase = true)
        .replace("Closed", context.getString(R.string.closed).lowercase(), ignoreCase = true)
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