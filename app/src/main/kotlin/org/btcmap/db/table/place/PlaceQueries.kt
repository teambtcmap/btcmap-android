package org.btcmap.db.table.place

import android.database.sqlite.SQLiteDatabase
import org.btcmap.db.table.place.PlaceProjectionFull.Companion.fromCursor
import java.time.ZonedDateTime

class PlaceQueries(val db: SQLiteDatabase) {

    fun insert(rows: List<Place>) {
        val sql = """
            INSERT OR REPLACE INTO ${PlaceSchema.NAME} (${Place.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21, ?22, ?23)
        """

        val stmt = db.compileStatement(sql)

        stmt.use {
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindLong(2, if (row.bundled) 1 else 0)
                stmt.bindString(3, row.updatedAt.toString())
                stmt.bindDouble(4, row.lat)
                stmt.bindDouble(5, row.lon)
                stmt.bindString(6, row.icon)
                if (row.name == null) {
                    stmt.bindNull(7)
                } else {
                    stmt.bindString(7, row.name)
                }
                if (row.localizedName == null) {
                    stmt.bindNull(8)
                } else {
                    stmt.bindString(8, row.localizedName.toString())
                }
                if (row.verifiedAt == null) {
                    stmt.bindNull(9)
                } else {
                    stmt.bindString(9, row.verifiedAt.toString())
                }
                if (row.address == null) {
                    stmt.bindNull(10)
                } else {
                    stmt.bindString(10, row.address)
                }
                if (row.openingHours == null) {
                    stmt.bindNull(11)
                } else {
                    stmt.bindString(11, row.openingHours)
                }
                if (row.localizedOpeningHours == null) {
                    stmt.bindNull(12)
                } else {
                    stmt.bindString(12, row.localizedOpeningHours.toString())
                }
                if (row.phone == null) {
                    stmt.bindNull(13)
                } else {
                    stmt.bindString(13, row.phone)
                }
                if (row.website == null) {
                    stmt.bindNull(14)
                } else {
                    stmt.bindString(14, row.website.toString())
                }
                if (row.email == null) {
                    stmt.bindNull(15)
                } else {
                    stmt.bindString(15, row.email)
                }
                if (row.twitter == null) {
                    stmt.bindNull(16)
                } else {
                    stmt.bindString(16, row.twitter.toString())
                }
                if (row.facebook == null) {
                    stmt.bindNull(17)
                } else {
                    stmt.bindString(17, row.facebook.toString())
                }
                if (row.instagram == null) {
                    stmt.bindNull(18)
                } else {
                    stmt.bindString(18, row.instagram.toString())
                }
                if (row.line == null) {
                    stmt.bindNull(19)
                } else {
                    stmt.bindString(19, row.line.toString())
                }
                if (row.requiredAppUrl == null) {
                    stmt.bindNull(20)
                } else {
                    stmt.bindString(20, row.requiredAppUrl.toString())
                }
                if (row.boostedUntil == null) {
                    stmt.bindNull(21)
                } else {
                    stmt.bindString(21, row.boostedUntil.toString())
                }
                if (row.comments == null) {
                    stmt.bindNull(22)
                } else {
                    stmt.bindLong(22, row.comments)
                }
                if (row.telegram == null) {
                    stmt.bindNull(23)
                } else {
                    stmt.bindString(23, row.telegram.toString())
                }
                stmt.executeInsert()
            }
        }
    }

    fun selectById(id: Long): Place {
        val cursor = db.rawQuery(
            """
                SELECT ${Place.columns}
                FROM ${PlaceSchema.NAME}
                WHERE ${PlaceSchema.Columns.Id} = ?1
            """,
            arrayOf(id.toString())
        )

        cursor.moveToFirst()
        return fromCursor(cursor)
    }

    fun selectBySearchString(searchString: String): List<Place> {
        val cursor = db.rawQuery(
            """
                SELECT ${Place.columns}
                FROM ${PlaceSchema.NAME}
                WHERE UPPER(${PlaceSchema.Columns.Name}) LIKE '%' || UPPER(?1) || '%'
            """,
            arrayOf(searchString),
        )

        cursor.use {
            val rows = mutableListOf<Place>()

            while (cursor.moveToNext()) {
                rows.add(fromCursor(cursor))
            }

            return rows
        }
    }

    fun selectMerchants(): List<Cluster> {
        val sql = """
            SELECT
                ${PlaceSchema.Columns.Id},
                ${PlaceSchema.Columns.Lat},
                ${PlaceSchema.Columns.Lon},
                ${PlaceSchema.Columns.Icon},
                ${PlaceSchema.Columns.BoostedUntil},
                ${PlaceSchema.Columns.RequiredAppUrl},
                ${PlaceSchema.Columns.Comments}
            FROM ${PlaceSchema.NAME}
            WHERE
                ${PlaceSchema.Columns.Icon} <> 'local_atm' AND ${PlaceSchema.Columns.Icon} <> 'currency_exchange'
            ORDER BY ${PlaceSchema.Columns.Lat} DESC
        """

        val cursor = db.rawQuery(sql, null)

        cursor.use {
            val rows = mutableListOf<Cluster>()

            while (cursor.moveToNext()) {
                rows.add(
                    Cluster(
                        count = 1,
                        id = it.getLong(0),
                        lat = it.getDouble(1),
                        lon = it.getDouble(2),
                        iconId = it.getString(3),
                        boostExpires = if (it.isNull(4)) null else ZonedDateTime.parse(
                            it.getString(
                                4,
                            )
                        ),
                        requiresCompanionApp = !it.isNull(5),
                        comments = if (it.isNull(6)) 0 else it.getLong(6),
                    )
                )
            }

            return rows
        }
    }

    fun selectExchanges(): List<Cluster> {
        val sql = """
            SELECT
                ${PlaceSchema.Columns.Id},
                ${PlaceSchema.Columns.Lat},
                ${PlaceSchema.Columns.Lon},
                ${PlaceSchema.Columns.Icon},
                ${PlaceSchema.Columns.BoostedUntil},
                ${PlaceSchema.Columns.RequiredAppUrl},
                ${PlaceSchema.Columns.Comments}
            FROM ${PlaceSchema.NAME}
            WHERE
                ${PlaceSchema.Columns.Icon} = 'local_atm' OR ${PlaceSchema.Columns.Icon} = 'currency_exchange'
            ORDER BY ${PlaceSchema.Columns.Lat} DESC
        """

        val cursor = db.rawQuery(sql, null)

        cursor.use {
            val rows = mutableListOf<Cluster>()

            while (cursor.moveToNext()) {
                rows.add(
                    Cluster(
                        count = 1,
                        id = it.getLong(0),
                        lat = it.getDouble(1),
                        lon = it.getDouble(2),
                        iconId = it.getString(3),
                        boostExpires = if (it.isNull(4)) null else ZonedDateTime.parse(
                            it.getString(
                                4,
                            )
                        ),
                        requiresCompanionApp = !it.isNull(5),
                        comments = if (it.isNull(6)) 0 else it.getLong(6),
                    )
                )
            }

            return rows
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        db.rawQuery("SELECT max(${PlaceSchema.Columns.UpdatedAt}) FROM ${PlaceSchema.NAME}", null)
            .use {
                it.moveToFirst()
                return if (it.isNull(0)) null else ZonedDateTime.parse(it.getString(0))
            }
    }

    fun selectCount(): Long {
        db.rawQuery("SELECT count(*) FROM ${PlaceSchema.NAME}", null).use {
            it.moveToFirst()
            return it.getLong(0)
        }
    }

    fun deleteById(id: Long): Int {
        return db.delete(
            PlaceSchema.NAME,
            "${PlaceSchema.Columns.Id.sqlName} = ?1",
            arrayOf(id.toString()),
        )
    }
}