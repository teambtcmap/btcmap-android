package org.btcmap.db.table.place

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindHttpUrlOrNull
import org.btcmap.db.bindJsonObjectOrNull
import org.btcmap.db.bindLongOrNull
import org.btcmap.db.bindTextOrNull
import org.btcmap.db.bindZonedDateTime
import org.btcmap.db.bindZonedDateTimeOrNull
import org.btcmap.db.escapeLikePattern
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

/**
 * Caps the number of bound variables per statement. Android 10 (minSdk 29)
 * ships SQLite with a limit of 999, and [PlaceQueries.selectByOsmIds] binds one
 * per id, so it splits large inputs into chunks below that.
 */
private const val MAX_QUERY_VARIABLES = 900

class PlaceQueries(private val conn: SQLiteConnection) {
    fun insert(rows: List<Place>) {
        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE ($ID, $UPDATED_AT, $LAT, $LON, $ICON, $NAME, $LOCALIZED_NAME, $VERIFIED_AT, $ADDRESS, $OPENING_HOURS, $LOCALIZED_OPENING_HOURS, $PHONE, $WEBSITE, $EMAIL, $TWITTER, $FACEBOOK, $INSTAGRAM, $LINE, $REQUIRED_APP_URL, $BOOSTED_UNTIL, $COMMENTS, $TELEGRAM, $OSM_ID, $DELETED_AT)
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21, ?22, ?23, ?24);
            """
        ).use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindZonedDateTime(2, row.updatedAt)
                stmt.bindDouble(3, row.lat)
                stmt.bindDouble(4, row.lon)
                stmt.bindText(5, row.icon)
                stmt.bindTextOrNull(6, row.name)
                stmt.bindJsonObjectOrNull(7, row.localizedName)
                stmt.bindZonedDateTimeOrNull(8, row.verifiedAt)
                stmt.bindTextOrNull(9, row.address)
                stmt.bindTextOrNull(10, row.openingHours)
                stmt.bindJsonObjectOrNull(11, row.localizedOpeningHours)
                stmt.bindTextOrNull(12, row.phone)
                stmt.bindHttpUrlOrNull(13, row.website)
                stmt.bindTextOrNull(14, row.email)
                stmt.bindHttpUrlOrNull(15, row.twitter)
                stmt.bindHttpUrlOrNull(16, row.facebook)
                stmt.bindHttpUrlOrNull(17, row.instagram)
                stmt.bindHttpUrlOrNull(18, row.line)
                stmt.bindHttpUrlOrNull(19, row.requiredAppUrl)
                stmt.bindZonedDateTimeOrNull(20, row.boostedUntil)
                stmt.bindLongOrNull(21, row.comments)
                stmt.bindHttpUrlOrNull(22, row.telegram)
                stmt.bindTextOrNull(23, row.osmId)
                stmt.bindZonedDateTimeOrNull(24, row.deletedAt)
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
                WHERE $ID = ?1 AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindLong(1, id)
            if (it.step()) {
                return FullProjection.fromStatement(it)
            }
            return null
        }
    }

    fun selectByOsmId(osmId: String): Place? {
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE $OSM_ID = ?1 AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindText(1, osmId)
            if (it.step()) {
                return FullProjection.fromStatement(it)
            }
            return null
        }
    }

    /**
     * Places matching any of the given OSM ids, keyed by their OSM id. Ids with
     * no matching place are simply absent from the result.
     *
     * A single query, so a caller resolving the places behind a list of issues
     * does not run one query per row. The ids are chunked so a very long list
     * does not exceed SQLite's bound-variable limit, and the chunks' results are
     * merged.
     */
    fun selectByOsmIds(osmIds: Collection<String>): Map<String, Place> {
        val ids = osmIds.toSet()
        if (ids.isEmpty()) return emptyMap()

        val places = mutableMapOf<String, Place>()
        for (chunk in ids.chunked(MAX_QUERY_VARIABLES)) {
            val placeholders = chunk.indices.joinToString(", ") { "?${it + 1}" }
            conn.prepare(
                """
                    SELECT ${FullProjection.COLUMNS}
                    FROM $TABLE
                    WHERE $OSM_ID IN ($placeholders) AND $DELETED_AT IS NULL;
                """
            ).use { stmt ->
                chunk.forEachIndexed { index, osmId -> stmt.bindText(index + 1, osmId) }
                while (stmt.step()) {
                    val place = FullProjection.fromStatement(stmt)
                    place.osmId?.let { places[it] = place }
                }
            }
        }
        return places
    }

    fun selectBySearchString(searchString: String): List<Place> {
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE UPPER($NAME) LIKE '%' || UPPER(?1) || '%' ESCAPE '\'
                    AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindText(1, searchString.escapeLikePattern())
            val rows = mutableListOf<Place>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    /**
     * Full places inside the bounding box, ordered by latitude descending like
     * [selectMerchantsByBounds]. When [withBoost] is true only places that carry
     * a boost timestamp are returned, so a caller looking for active boosts does
     * not load every place in a large area; it still checks that the boost is
     * active. The caller also narrows the box with the area's point-in-polygon
     * test where the association has to match the server's.
     */
    fun selectByBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        withBoost: Boolean = false,
    ): List<Place> {
        val boostClause = if (withBoost) " AND $BOOSTED_UNTIL IS NOT NULL" else ""
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE $LAT >= ?1 AND $LAT <= ?2 AND $LON >= ?3 AND $LON <= ?4
                    AND $DELETED_AT IS NULL$boostClause
                ORDER BY $LAT DESC;
            """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)
            val rows = mutableListOf<Place>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
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
            append(" AND $DELETED_AT IS NULL")
            append(" AND $LAT >= ?1 AND $LAT <= ?2 AND $LON >= ?3 AND $LON <= ?4")
            if (minVerifiedAt != null) {
                // julianday, not plain text: timestamps are stored as
                // ZonedDateTime.toString(), which is not fixed-width, so text
                // ordering is not chronological (see selectMaxUpdatedAt).
                append(" AND julianday($VERIFIED_AT) >= julianday(?5)")
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
                    ($ICON = 'local_atm' OR $ICON = 'currency_exchange')
                    AND $DELETED_AT IS NULL
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
                    AND $DELETED_AT IS NULL
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
        // See CommentQueries.selectMaxUpdatedAt: text ordering of
        // ZonedDateTime.toString() values is not chronological.
        conn.prepare(
            """
            SELECT $UPDATED_AT
            FROM $TABLE
            ORDER BY julianday($UPDATED_AT) DESC
            LIMIT 1;
            """
        ).use {
            if (!it.step()) {
                return null
            }
            return it.getZonedDateTimeOrNull(0)
        }
    }

    /**
     * Row count for the table. Tombstones are excluded by default; callers that
     * need to know whether the table is populated at all (the bundled seed
     * guard) pass [includeDeleted] = true, because a table that holds only
     * deleted places is still already populated and re-seeding it would
     * resurrect places that were deleted after the snapshot was built.
     */
    fun selectCount(includeDeleted: Boolean = false): Long {
        val where = if (includeDeleted) "" else " WHERE $DELETED_AT IS NULL"
        conn.prepare("SELECT count(*) FROM $TABLE$where;").use {
            it.step()
            return it.getLong(0)
        }
    }
}