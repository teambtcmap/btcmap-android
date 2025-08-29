package element

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import db.getHttpUrlOrNull
import db.getTextOrNull
import db.getZonedDateTimeOrNull
import db.transaction
import java.time.ZonedDateTime

class ElementQueries(private val conn: SQLiteConnection) {

    companion object {
        const val TABLE_NAME = "element"

        const val COL_ID = "id"
        const val COL_LAT = "lat"
        const val COL_LON = "lon"
        const val COL_ICON = "icon"
        const val COL_NAME = "name"
        const val COL_UPDATED_AT = "updated_at"
        const val COL_DELETED_AT = "deleted_at"
        const val COL_REQUIRED_APP_URL = "required_app_url"
        const val COL_BOOSTED_UNTIL = "boosted_until"
        const val COL_VERIFIED_AT = "verified_at"
        const val COL_ADDRESS = "address"
        const val COL_OPENING_HOURS = "opening_hours"
        const val COL_WEBSITE = "website"
        const val COL_PHONE = "phone"
        const val COL_EMAIL = "email"
        const val COL_TWITTER = "twitter"
        const val COL_FACEBOOK = "facebook"
        const val COL_INSTAGRAM = "instagram"
        const val COL_LINE = "line"
        const val COL_BUNDLED = "bundled"
        const val COL_COMMENTS = "comments"

        val PROJ_FULL = listOf(
            COL_ID,
            COL_LAT,
            COL_LON,
            COL_ICON,
            COL_NAME,
            COL_UPDATED_AT,
            COL_DELETED_AT,
            COL_REQUIRED_APP_URL,
            COL_BOOSTED_UNTIL,
            COL_VERIFIED_AT,
            COL_ADDRESS,
            COL_OPENING_HOURS,
            COL_WEBSITE,
            COL_PHONE,
            COL_EMAIL,
            COL_TWITTER,
            COL_FACEBOOK,
            COL_INSTAGRAM,
            COL_LINE,
            COL_BUNDLED,
            COL_COMMENTS,
        )

        const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER NOT NULL PRIMARY KEY,
                $COL_LAT REAL NOT NULL,
                $COL_LON REAL NOT NULL,
                $COL_ICON TEXT NOT NULL,
                $COL_NAME TEXT NOT NULL,
                $COL_UPDATED_AT TEXT NOT NULL,
                $COL_DELETED_AT TEXT,
                $COL_REQUIRED_APP_URL TEXT,
                $COL_BOOSTED_UNTIL TEXT,
                $COL_VERIFIED_AT TEXT,
                $COL_ADDRESS TEXT,
                $COL_OPENING_HOURS TEXT,
                $COL_WEBSITE TEXT,
                $COL_PHONE TEXT,
                $COL_EMAIL TEXT,
                $COL_TWITTER TEXT,
                $COL_FACEBOOK TEXT,
                $COL_INSTAGRAM TEXT,
                $COL_LINE TEXT,
                $COL_BUNDLED TEXT,
                $COL_COMMENTS INTEGER NOT NULL
            );
            """
    }

    fun insertOrReplace(elements: List<Element>) {
        val sql = """
            INSERT OR REPLACE
            INTO $TABLE_NAME (${PROJ_FULL.joinToString()}) 
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21)
        """
        conn.transaction { conn ->
            elements.forEach { element ->
                conn.prepare(sql).use {
                    it.bindLong(1, element.id)
                    it.bindDouble(2, element.lat)
                    it.bindDouble(3, element.lon)
                    it.bindText(4, element.icon)
                    it.bindText(5, element.name)
                    it.bindText(6, element.updatedAt.toString())
                    if (element.deletedAt == null) {
                        it.bindNull(7)
                    } else {
                        it.bindText(7, element.deletedAt.toString())
                    }
                    if (element.requiredAppUrl == null) {
                        it.bindNull(8)
                    } else {
                        it.bindText(8, element.requiredAppUrl.toString())
                    }
                    if (element.boostedUntil == null) {
                        it.bindNull(9)
                    } else {
                        it.bindText(9, element.boostedUntil.toString())
                    }
                    if (element.verifiedAt == null) {
                        it.bindNull(10)
                    } else {
                        it.bindText(10, element.verifiedAt.toString())
                    }
                    if (element.address == null) {
                        it.bindNull(11)
                    } else {
                        it.bindText(11, element.address)
                    }
                    if (element.openingHours == null) {
                        it.bindNull(12)
                    } else {
                        it.bindText(12, element.openingHours)
                    }
                    if (element.website == null) {
                        it.bindNull(13)
                    } else {
                        it.bindText(13, element.website.toString())
                    }
                    if (element.phone == null) {
                        it.bindNull(14)
                    } else {
                        it.bindText(14, element.phone)
                    }
                    if (element.email == null) {
                        it.bindNull(15)
                    } else {
                        it.bindText(15, element.email)
                    }
                    if (element.twitter == null) {
                        it.bindNull(16)
                    } else {
                        it.bindText(16, element.twitter.toString())
                    }
                    if (element.facebook == null) {
                        it.bindNull(17)
                    } else {
                        it.bindText(17, element.facebook.toString())
                    }
                    if (element.instagram == null) {
                        it.bindNull(18)
                    } else {
                        it.bindText(18, element.instagram.toString())
                    }
                    if (element.line == null) {
                        it.bindNull(19)
                    } else {
                        it.bindText(19, element.line.toString())
                    }
                    it.bindBoolean(20, element.bundled)
                    it.bindLong(21, element.comments)
                    it.step()
                }
            }
        }
    }

    fun selectById(id: Long): Element? {
        return conn.prepare(
            """
                SELECT ${PROJ_FULL.joinToString()}
                FROM element
                WHERE $COL_ID = ?1
                """
        ).use {
            it.bindLong(1, id)
            if (it.step()) {
                it.toElement()
            } else {
                null
            }
        }
    }

    fun selectBySearchString(searchString: String): List<Element> {
        return conn.prepare(
            """
                SELECT ${PROJ_FULL.joinToString()}
                FROM element
                WHERE UPPER($COL_NAME) LIKE '%' || UPPER(?1) || '%'
                """
        ).use {
            it.bindText(1, searchString)
            buildList {
                while (it.step()) {
                    add(it.toElement())
                }
            }
        }
    }

    fun selectWithoutClustering(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<ElementsCluster> {
        return conn.prepare(
            """
                SELECT
                    $COL_ID,
                    $COL_LAT,
                    $COL_LON,
                    $COL_ICON,
                    $COL_BOOSTED_UNTIL,
                    $COL_REQUIRED_APP_URL,
                    $COL_COMMENTS
                FROM $TABLE_NAME
                WHERE
                    $COL_ICON <> 'local_atm'
                    AND $COL_ICON <> 'currency_exchange'
                    AND $COL_LAT > ?1
                    AND $COL_LAT < ?2
                    AND $COL_LON > ?3
                    AND $COL_LON < ?4
                ORDER BY $COL_LAT DESC
                """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)
            buildList {
                while (it.step()) {
                    add(
                        ElementsCluster(
                            count = 1,
                            id = it.getLong(0),
                            lat = it.getDouble(1),
                            lon = it.getDouble(2),
                            iconId = it.getText(3),
                            boostExpires = it.getZonedDateTimeOrNull(4),
                            requiresCompanionApp = it.getHttpUrlOrNull(5) != null,
                            comments = it.getLong(6),
                        )
                    )
                }
            }
        }
    }

    fun selectExchangesWithoutClustering(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<ElementsCluster> {
        return conn.prepare(
            """
                SELECT
                    $COL_ID,
                    $COL_LAT,
                    $COL_LON,
                    $COL_ICON,
                    $COL_BOOSTED_UNTIL,
                    $COL_REQUIRED_APP_URL,
                    $COL_COMMENTS
                FROM $TABLE_NAME
                WHERE
                    ($COL_ICON = 'local_atm' OR $COL_ICON = 'currency_exchange')
                    AND $COL_LAT > ?1
                    AND $COL_LAT < ?2
                    AND $COL_LON > ?3
                    AND $COL_LON < ?4
                ORDER BY $COL_LAT DESC
                """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)
            buildList {
                while (it.step()) {
                    add(
                        ElementsCluster(
                            count = 1,
                            id = it.getLong(0),
                            lat = it.getDouble(1),
                            lon = it.getDouble(2),
                            iconId = it.getText(3),
                            boostExpires = it.getZonedDateTimeOrNull(4),
                            requiresCompanionApp = it.getHttpUrlOrNull(5) != null,
                            comments = it.getLong(6),
                        )
                    )
                }
            }
        }
    }

    fun selectClusters(
        stepLat: Double,
        stepLon: Double,
    ): List<ElementsCluster> {
        return conn.prepare(
            """
                SELECT
                    count(*),
                    $COL_ID,
                    avg($COL_LAT) AS $COL_LAT,
                    avg($COL_LON) AS $COL_LON,
                    $COL_ICON,
                    $COL_BOOSTED_UNTIL,
                    $COL_REQUIRED_APP_URL,
                    $COL_COMMENTS
                FROM $TABLE_NAME
                WHERE ($COL_ICON <> 'local_atm' AND $COL_ICON <> 'currency_exchange')
                GROUP BY round($COL_LAT / ?1) * ?1, round($COL_LON / ?2) * ?2
                ORDER BY $COL_LAT DESC
                """
        ).use {
            it.bindDouble(1, stepLat)
            it.bindDouble(2, stepLon)
            buildList {
                while (it.step()) {
                    add(
                        ElementsCluster(
                            count = it.getLong(0),
                            id = it.getLong(1),
                            lat = it.getDouble(2),
                            lon = it.getDouble(3),
                            iconId = it.getText(4),
                            boostExpires = it.getZonedDateTimeOrNull(5),
                            requiresCompanionApp = it.getHttpUrlOrNull(6) != null,
                            comments = it.getLong(7),
                        )
                    )
                }
            }
        }
    }

    fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<Element> {
        return conn.prepare(
            """
                SELECT ${PROJ_FULL.joinToString()}
                FROM $TABLE_NAME
                WHERE $COL_LAT > ?1 AND $COL_LAT < ?2 AND $COL_LON > ?3 AND $COL_LON < ?4
                """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)
            buildList {
                while (it.step()) {
                    add(it.toElement())
                }
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        return conn.prepare("SELECT max($COL_UPDATED_AT) FROM $TABLE_NAME").use {
            if (it.step()) {
                it.getZonedDateTimeOrNull(0)
            } else {
                null
            }
        }
    }

    fun selectCount(): Long {
        return conn.prepare("SELECT count(*) FROM $TABLE_NAME").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM $TABLE_NAME WHERE $COL_ID = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }

    private fun SQLiteStatement.toElement(): Element {
        return Element(
            id = getLong(0),
            lat = getDouble(1),
            lon = getDouble(2),
            icon = getText(3),
            name = getText(4),
            updatedAt = getText(5),
            deletedAt = getTextOrNull(6),
            requiredAppUrl = getTextOrNull(7),
            boostedUntil = getTextOrNull(8),
            verifiedAt = getTextOrNull(9),
            address = getTextOrNull(10),
            openingHours = getTextOrNull(11),
            website = getTextOrNull(12),
            phone = getTextOrNull(13),
            email = getTextOrNull(14),
            twitter = getTextOrNull(15),
            facebook = getTextOrNull(16),
            instagram = getTextOrNull(17),
            line = getTextOrNull(18),
            bundled = getBoolean(19),
            comments = getLong(20),
        )
    }
}