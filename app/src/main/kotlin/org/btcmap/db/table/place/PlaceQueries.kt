package org.btcmap.db.table.place

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindHttpUrlOrNull
import org.btcmap.db.bindJsonObjectOrNull
import org.btcmap.db.bindLongOrNull
import org.btcmap.db.bindTextOrNull
import org.btcmap.db.bindZonedDateTime
import org.btcmap.db.bindZonedDateTimeOrNull
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

class PlaceQueries(private val conn: SQLiteConnection) {
    fun insert(rows: List<Place>) {
        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE ($ID, $BUNDLED, $UPDATED_AT, $LAT, $LON, $ICON, $NAME, $LOCALIZED_NAME, $VERIFIED_AT, $ADDRESS, $OPENING_HOURS, $LOCALIZED_OPENING_HOURS, $PHONE, $WEBSITE, $EMAIL, $TWITTER, $FACEBOOK, $INSTAGRAM, $LINE, $REQUIRED_APP_URL, $BOOSTED_UNTIL, $COMMENTS, $TELEGRAM)
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21, ?22, ?23);
            """
        ).use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindBoolean(2, row.bundled)
                stmt.bindZonedDateTime(3, row.updatedAt)
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
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE $ID = ?1;
            """
        ).use {
            it.bindLong(1, id)
            if (it.step()) {
                return FullProjection.fromStatement(it)
            }
            return null
        }
    }

    fun selectBySearchString(searchString: String): List<Place> {
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE UPPER($NAME) LIKE '%' || UPPER(?1) || '%';
            """
        ).use {
            it.bindText(1, searchString)
            val rows = mutableListOf<Place>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectMerchants(): List<Marker> {
        conn.prepare(
            """
                SELECT ${MarkerProjection.COLUMNS}
                FROM $TABLE
                WHERE
                    $ICON <> 'local_atm' AND $ICON <> 'currency_exchange'
                ORDER BY $LAT DESC;
            """
        ).use {
            val rows = mutableListOf<Marker>()
            while (it.step()) {
                rows.add(MarkerProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectMerchantsByBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        minVerifiedAt: ZonedDateTime? = null,
    ): List<Marker> {
        val whereClause = buildString {
            append("$ICON <> 'local_atm' AND $ICON <> 'currency_exchange'")
            append(" AND $LAT >= ?1 AND $LAT <= ?2 AND $LON >= ?3 AND $LON <= ?4")
            if (minVerifiedAt != null) {
                append("AND ($BUNDLED = 1 OR $VERIFIED_AT >= ?5)")
            }
        }
        conn.prepare(
            """
                SELECT ${MarkerProjection.COLUMNS}
                FROM $TABLE
                WHERE $whereClause
                ORDER BY $LAT DESC;
            """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)
            if (minVerifiedAt != null) {
                it.bindText(5, minVerifiedAt.toString())
            }
            val rows = mutableListOf<Marker>()
            while (it.step()) {
                rows.add(MarkerProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectExchanges(): List<Marker> {
        conn.prepare(
            """
                SELECT ${MarkerProjection.COLUMNS}
                FROM $TABLE
                WHERE
                    $ICON = 'local_atm' OR $ICON = 'currency_exchange'
                ORDER BY $LAT DESC;
            """
        ).use {
            val rows = mutableListOf<Marker>()
            while (it.step()) {
                rows.add(MarkerProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectExchangesByBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<Marker> {
        conn.prepare(
            """
                SELECT ${MarkerProjection.COLUMNS}
                FROM $TABLE
                WHERE
                    ($ICON = 'local_atm' OR $ICON = 'currency_exchange')
                    AND $LAT >= ?1 AND $LAT <= ?2 AND $LON >= ?3 AND $LON <= ?4
                ORDER BY $LAT DESC;
            """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)
            val rows = mutableListOf<Marker>()
            while (it.step()) {
                rows.add(MarkerProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        conn.prepare("SELECT max($UPDATED_AT) FROM $TABLE;").use {
            it.step()
            return it.getZonedDateTimeOrNull(0)
        }
    }

    fun selectCount(): Long {
        conn.prepare("SELECT count(*) FROM $TABLE;").use {
            it.step()
            return it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM $TABLE WHERE $ID = ?1;").use {
            it.bindLong(1, id)
            it.step()
        }
    }
}