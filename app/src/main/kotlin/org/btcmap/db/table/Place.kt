package org.btcmap.db.table

import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.R
import org.json.JSONObject
import java.time.ZonedDateTime
import java.util.Locale

object PlaceSchema {
    const val TABLE_NAME = "place"

    override fun toString(): String {
        return """
            CREATE TABLE $TABLE_NAME (
                ${Columns.Id} INTEGER PRIMARY KEY NOT NULL,
                ${Columns.Bundled} INTEGER NOT NULL,
                ${Columns.UpdatedAt} TEXT NOT NULL,
                ${Columns.Lat} REAL NOT NULL,
                ${Columns.Lon} REAL NOT NULL,
                ${Columns.Icon} TEXT NOT NULL,
                ${Columns.Name} TEXT,
                ${Columns.LocalizedName} TEXT,
                ${Columns.VerifiedAt} TEXT,
                ${Columns.Address} TEXT,
                ${Columns.OpeningHours} TEXT,
                ${Columns.LocalizedOpeningHours} TEXT,
                ${Columns.Phone} TEXT,
                ${Columns.Website} TEXT,
                ${Columns.Email} TEXT,
                ${Columns.Twitter} TEXT,
                ${Columns.Facebook} TEXT,
                ${Columns.Instagram} TEXT,
                ${Columns.Line} TEXT,                
                ${Columns.RequiredAppUrl} TEXT,
                ${Columns.BoostedUntil} TEXT,
                ${Columns.Comments} INTEGER,
                ${Columns.Telegram} TEXT;
            )
        """
    }

    enum class Columns(val sqlName: String) {
        Id("id"),
        Bundled("bundled"),
        UpdatedAt("updated_at"),
        Lat("lat"),
        Lon("lon"),
        Icon("icon"),
        Name("name"),
        LocalizedName("localized_name"),
        VerifiedAt("verified_at"),
        Address("address"),
        OpeningHours("opening_hours"),
        LocalizedOpeningHours("localized_opening_hours"),
        Phone("phone"),
        Website("website"),
        Email("email"),
        Twitter("twitter"),
        Facebook("facebook"),
        Instagram("instagram"),
        Line("line"),
        RequiredAppUrl("required_app_url"),
        BoostedUntil("boosted_until"),
        Comments("comments"),
        Telegram("telegram");

        override fun toString() = sqlName
    }
}

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

class PlaceQueries(private val conn: SQLiteConnection) {

    fun insert(rows: List<Place>) {
        val sql = """
            INSERT OR REPLACE INTO ${PlaceSchema.TABLE_NAME} (${Place.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21, ?22, ?23)
        """

        val stmt = conn.prepare(sql)

        stmt.use {
            rows.forEach { row ->
                it.bindLong(1, row.id)
                it.bindLong(2, if (row.bundled) 1 else 0)
                it.bindText(3, row.updatedAt.toString())
                it.bindDouble(4, row.lat)
                it.bindDouble(5, row.lon)
                it.bindText(6, row.icon)
                if (row.name == null) {
                    it.bindNull(7)
                } else {
                    it.bindText(7, row.name)
                }
                if (row.localizedName == null) {
                    it.bindNull(8)
                } else {
                    it.bindText(8, row.localizedName.toString())
                }
                if (row.verifiedAt == null) {
                    it.bindNull(9)
                } else {
                    it.bindText(9, row.verifiedAt.toString())
                }
                if (row.address == null) {
                    it.bindNull(10)
                } else {
                    it.bindText(10, row.address)
                }
                if (row.openingHours == null) {
                    it.bindNull(11)
                } else {
                    it.bindText(11, row.openingHours)
                }
                if (row.localizedOpeningHours == null) {
                    it.bindNull(12)
                } else {
                    it.bindText(12, row.localizedOpeningHours.toString())
                }
                if (row.phone == null) {
                    it.bindNull(13)
                } else {
                    it.bindText(13, row.phone)
                }
                if (row.website == null) {
                    it.bindNull(14)
                } else {
                    it.bindText(14, row.website.toString())
                }
                if (row.email == null) {
                    it.bindNull(15)
                } else {
                    it.bindText(15, row.email)
                }
                if (row.twitter == null) {
                    it.bindNull(16)
                } else {
                    it.bindText(16, row.twitter.toString())
                }
                if (row.facebook == null) {
                    it.bindNull(17)
                } else {
                    it.bindText(17, row.facebook.toString())
                }
                if (row.instagram == null) {
                    it.bindNull(18)
                } else {
                    it.bindText(18, row.instagram.toString())
                }
                if (row.line == null) {
                    it.bindNull(19)
                } else {
                    it.bindText(19, row.line.toString())
                }
                if (row.requiredAppUrl == null) {
                    it.bindNull(20)
                } else {
                    it.bindText(20, row.requiredAppUrl.toString())
                }
                if (row.boostedUntil == null) {
                    it.bindNull(21)
                } else {
                    it.bindText(21, row.boostedUntil.toString())
                }
                if (row.comments == null) {
                    it.bindNull(22)
                } else {
                    it.bindLong(22, row.comments)
                }
                if (row.telegram == null) {
                    it.bindNull(23)
                } else {
                    it.bindText(23, row.telegram.toString())
                }
                it.step()
            }
        }
    }

    fun selectById(id: Long): Place {
        val stmt = conn.prepare(
            """
                SELECT ${Place.columns}
                FROM ${PlaceSchema.TABLE_NAME}
                WHERE ${PlaceSchema.Columns.Id} = ?1
            """
        )
        stmt.bindLong(1, id)

        stmt.use {
            it.step()
            return PlaceProjectionFull.fromStatement(it)
        }
    }

    fun selectBySearchString(searchString: String): List<Place> {
        val stmt = conn.prepare(
            """
                SELECT ${Place.columns}
                FROM ${PlaceSchema.TABLE_NAME}
                WHERE UPPER(${PlaceSchema.Columns.Name}) LIKE '%' || UPPER(?1) || '%'
            """
        )
        stmt.bindText(1, searchString)

        stmt.use {
            val rows = mutableListOf<Place>()

            while (it.step()) {
                rows.add(PlaceProjectionFull.fromStatement(it))
            }

            return rows
        }
    }

    fun selectMerchants(): List<PlaceProjectionCluster> {
        val stmt = conn.prepare(
            """
                SELECT
                    ${PlaceSchema.Columns.Id},
                    ${PlaceSchema.Columns.Lat},
                    ${PlaceSchema.Columns.Lon},
                    ${PlaceSchema.Columns.Icon},
                    ${PlaceSchema.Columns.BoostedUntil},
                    ${PlaceSchema.Columns.RequiredAppUrl},
                    ${PlaceSchema.Columns.Comments}
                FROM ${PlaceSchema.TABLE_NAME}
                WHERE
                    ${PlaceSchema.Columns.Icon} <> 'local_atm' AND ${PlaceSchema.Columns.Icon} <> 'currency_exchange'
                ORDER BY ${PlaceSchema.Columns.Lat} DESC
            """
        )

        stmt.use {
            val rows = mutableListOf<PlaceProjectionCluster>()

            while (it.step()) {
                rows.add(
                    PlaceProjectionCluster(
                        count = 1,
                        id = it.getLong(0),
                        lat = it.getDouble(1),
                        lon = it.getDouble(2),
                        iconId = it.getText(3),
                        boostExpires = if (it.isNull(4)) null else ZonedDateTime.parse(it.getText(4)),
                        requiresCompanionApp = !it.isNull(5),
                        comments = if (it.isNull(6)) 0 else it.getLong(6),
                    )
                )
            }

            return rows
        }
    }

    fun selectExchanges(): List<PlaceProjectionCluster> {
        val stmt = conn.prepare(
            """
                SELECT
                    ${PlaceSchema.Columns.Id},
                    ${PlaceSchema.Columns.Lat},
                    ${PlaceSchema.Columns.Lon},
                    ${PlaceSchema.Columns.Icon},
                    ${PlaceSchema.Columns.BoostedUntil},
                    ${PlaceSchema.Columns.RequiredAppUrl},
                    ${PlaceSchema.Columns.Comments}
                FROM ${PlaceSchema.TABLE_NAME}
                WHERE
                    ${PlaceSchema.Columns.Icon} = 'local_atm' OR ${PlaceSchema.Columns.Icon} = 'currency_exchange'
                ORDER BY ${PlaceSchema.Columns.Lat} DESC
            """
        )

        stmt.use {
            val rows = mutableListOf<PlaceProjectionCluster>()

            while (it.step()) {
                rows.add(
                    PlaceProjectionCluster(
                        count = 1,
                        id = it.getLong(0),
                        lat = it.getDouble(1),
                        lon = it.getDouble(2),
                        iconId = it.getText(3),
                        boostExpires = if (it.isNull(4)) null else ZonedDateTime.parse(it.getText(4)),
                        requiresCompanionApp = !it.isNull(5),
                        comments = if (it.isNull(6)) 0 else it.getLong(6),
                    )
                )
            }

            return rows
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        conn.prepare("SELECT max(${PlaceSchema.Columns.UpdatedAt}) FROM ${PlaceSchema.TABLE_NAME}").use {
            it.step()
            return if (it.isNull(0)) null else ZonedDateTime.parse(it.getText(0))
        }
    }

    fun selectCount(): Long {
        conn.prepare("SELECT count(*) FROM ${PlaceSchema.TABLE_NAME}").use {
            it.step()
            return it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM ${PlaceSchema.TABLE_NAME} WHERE ${PlaceSchema.Columns.Id.sqlName} = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }
}