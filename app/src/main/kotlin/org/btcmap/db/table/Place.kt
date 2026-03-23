package org.btcmap.db.table

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import org.btcmap.db.bindHttpUrlOrNull
import org.btcmap.db.bindJsonObjectOrNull
import org.btcmap.db.bindLongOrNull
import org.btcmap.db.bindTextOrNull
import org.btcmap.db.bindZonedDateTimeOrNull
import org.btcmap.db.getHttpUrlOrNull
import org.btcmap.db.getJsonObjectOrNull
import org.btcmap.db.getLongOrNull
import org.btcmap.db.getTextOrNull
import org.btcmap.db.getZonedDateTimeOrNull
import org.json.JSONObject
import java.time.ZonedDateTime

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
                ${Columns.Telegram} TEXT
            );
        """.trimIndent()
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

typealias Marker = PlaceProjectionMarker

data class PlaceProjectionMarker(
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
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21, ?22, ?23);
        """

        conn.prepare(sql).use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindLong(2, if (row.bundled) 1 else 0)
                stmt.bindText(3, row.updatedAt.toString())
                stmt.bindDouble(4, row.lat)
                stmt.bindDouble(5, row.lon)
                stmt.bindText(6, row.icon)
                stmt.bindTextOrNull(7, row.name)
                stmt.bindJsonObjectOrNull(8, row.localizedName)
                stmt.bindZonedDateTimeOrNull(9, row.verifiedAt)
                stmt.bindTextOrNull(10, row.address)
                stmt.bindTextOrNull(11, row.openingHours)
                stmt.bindJsonObjectOrNull(12, row.localizedOpeningHours)
                stmt.bindTextOrNull(13, row.phone)
                stmt.bindHttpUrlOrNull(14, row.website)
                stmt.bindTextOrNull(15, row.email)
                stmt.bindHttpUrlOrNull(16, row.twitter)
                stmt.bindHttpUrlOrNull(17, row.facebook)
                stmt.bindHttpUrlOrNull(18, row.instagram)
                stmt.bindHttpUrlOrNull(19, row.line)
                stmt.bindHttpUrlOrNull(20, row.requiredAppUrl)
                stmt.bindZonedDateTimeOrNull(21, row.boostedUntil)
                stmt.bindLongOrNull(22, row.comments)
                stmt.bindHttpUrlOrNull(23, row.telegram)
                stmt.step()
                stmt.reset()
            }
        }
    }

    fun selectById(id: Long): Place? {
        val stmt = conn.prepare(
            """
                SELECT ${Place.columns}
                FROM ${PlaceSchema.TABLE_NAME}
                WHERE ${PlaceSchema.Columns.Id} = ?1;
            """
        )
        stmt.bindLong(1, id)

        stmt.use {
            if (it.step()) {
                return PlaceProjectionFull.fromStatement(it)
            }
            return null
        }
    }

    fun selectBySearchString(searchString: String): List<Place> {
        val stmt = conn.prepare(
            """
                SELECT ${Place.columns}
                FROM ${PlaceSchema.TABLE_NAME}
                WHERE UPPER(${PlaceSchema.Columns.Name}) LIKE '%' || UPPER(?1) || '%';
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

    fun selectMerchants(): List<PlaceProjectionMarker> {
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
                ORDER BY ${PlaceSchema.Columns.Lat} DESC;
            """
        )

        stmt.use {
            val rows = mutableListOf<PlaceProjectionMarker>()

            while (it.step()) {
                rows.add(
                    PlaceProjectionMarker(
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

    fun selectExchanges(): List<PlaceProjectionMarker> {
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
                ORDER BY ${PlaceSchema.Columns.Lat} DESC;
            """
        )

        stmt.use {
            val rows = mutableListOf<PlaceProjectionMarker>()

            while (it.step()) {
                rows.add(
                    PlaceProjectionMarker(
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
        conn.prepare("SELECT max(${PlaceSchema.Columns.UpdatedAt}) FROM ${PlaceSchema.TABLE_NAME};")
            .use {
                it.step()
                return it.getZonedDateTimeOrNull(0)
            }
    }

    fun selectCount(): Long {
        conn.prepare("SELECT count(*) FROM ${PlaceSchema.TABLE_NAME};").use {
            it.step()
            return it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM ${PlaceSchema.TABLE_NAME} WHERE ${PlaceSchema.Columns.Id.sqlName} = ?1;")
            .use {
                it.bindLong(1, id)
                it.step()
            }
    }
}