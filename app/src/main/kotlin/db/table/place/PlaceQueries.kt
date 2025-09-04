package db.table.place

import android.database.sqlite.SQLiteDatabase
import db.table.place.PlaceProjectionFull.Companion.fromCursor
import java.time.ZonedDateTime

object PlaceQueries {
    fun insert(
        rows: List<Place>,
        db: SQLiteDatabase,
    ) {
        val sql = """
            INSERT OR REPLACE INTO ${PlaceSchema.NAME} (${Place.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20)
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
                if (row.verifiedAt == null) {
                    stmt.bindNull(8)
                } else {
                    stmt.bindString(8, row.verifiedAt.toString())
                }
                if (row.address == null) {
                    stmt.bindNull(9)
                } else {
                    stmt.bindString(9, row.address)
                }
                if (row.openingHours == null) {
                    stmt.bindNull(10)
                } else {
                    stmt.bindString(10, row.openingHours)
                }
                if (row.phone == null) {
                    stmt.bindNull(11)
                } else {
                    stmt.bindString(11, row.phone)
                }
                if (row.website == null) {
                    stmt.bindNull(12)
                } else {
                    stmt.bindString(12, row.website.toString())
                }
                if (row.email == null) {
                    stmt.bindNull(13)
                } else {
                    stmt.bindString(13, row.email)
                }
                if (row.twitter == null) {
                    stmt.bindNull(14)
                } else {
                    stmt.bindString(14, row.twitter.toString())
                }
                if (row.facebook == null) {
                    stmt.bindNull(15)
                } else {
                    stmt.bindString(15, row.facebook.toString())
                }
                if (row.instagram == null) {
                    stmt.bindNull(16)
                } else {
                    stmt.bindString(16, row.instagram.toString())
                }
                if (row.line == null) {
                    stmt.bindNull(17)
                } else {
                    stmt.bindString(17, row.line.toString())
                }
                if (row.requiredAppUrl == null) {
                    stmt.bindNull(18)
                } else {
                    stmt.bindString(18, row.requiredAppUrl.toString())
                }
                if (row.boostedUntil == null) {
                    stmt.bindNull(19)
                } else {
                    stmt.bindString(19, row.boostedUntil.toString())
                }
                if (row.comments == null) {
                    stmt.bindNull(20)
                } else {
                    stmt.bindLong(20, row.comments)
                }
                stmt.executeInsert()
            }
        }
    }

    fun selectById(id: Long, db: SQLiteDatabase): Place {
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

    fun selectBySearchString(searchString: String, db: SQLiteDatabase): List<Place> {
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

    fun selectClusters(
        stepLat: Double,
        stepLon: Double,
        includeMerchants: Boolean,
        includeExchanges: Boolean,
        db: SQLiteDatabase,
    ): List<Cluster> {
        val where = if (includeMerchants && includeExchanges) {
            ""
        } else if (includeMerchants) {
            "WHERE ${PlaceSchema.Columns.Icon} <> 'local_atm' AND ${PlaceSchema.Columns.Icon} <> 'currency_exchange'"
        } else if (includeExchanges) {
            "WHERE ${PlaceSchema.Columns.Icon} = 'local_atm' OR ${PlaceSchema.Columns.Icon} = 'currency_exchange'"
        } else {
            return emptyList()
        }

        val sql = """
            SELECT
                count(*),
                ${PlaceSchema.Columns.Id},
                avg(${PlaceSchema.Columns.Lat}) AS ${PlaceSchema.Columns.Lat},
                avg(${PlaceSchema.Columns.Lon}) AS ${PlaceSchema.Columns.Lon},
                ${PlaceSchema.Columns.Icon},
                ${PlaceSchema.Columns.BoostedUntil},
                ${PlaceSchema.Columns.RequiredAppUrl},
                ${PlaceSchema.Columns.Comments}
            FROM ${PlaceSchema.NAME}
            $where
            GROUP BY round(${PlaceSchema.Columns.Lat} / ?1) * ?1, round(${PlaceSchema.Columns.Lon} / ?2) * ?2
            ORDER BY ${PlaceSchema.Columns.Lat} DESC
        """

        val cursor = db.rawQuery(
            sql,
            arrayOf(stepLat.toString(), stepLon.toString())
        )

        cursor.use {
            val rows = mutableListOf<Cluster>()

            while (cursor.moveToNext()) {
                rows.add(
                    Cluster(
                        count = it.getLong(0),
                        id = it.getLong(1),
                        lat = it.getDouble(2),
                        lon = it.getDouble(3),
                        iconId = it.getString(4),
                        boostExpires = if (it.isNull(5)) null else ZonedDateTime.parse(
                            it.getString(
                                5,
                            )
                        ),
                        requiresCompanionApp = !it.isNull(6),
                        comments = if (it.isNull(7)) 0 else it.getLong(7),
                    )
                )
            }

            return rows
        }
    }

    fun selectWithoutClustering(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        includeMerchants: Boolean,
        includeExchanges: Boolean,
        db: SQLiteDatabase,
    ): List<Cluster> {
        val where = if (includeMerchants && includeExchanges) {
            ""
        } else if (includeMerchants) {
            "AND ${PlaceSchema.Columns.Icon} <> 'local_atm' AND ${PlaceSchema.Columns.Icon} <> 'currency_exchange'"
        } else if (includeExchanges) {
            "AND ${PlaceSchema.Columns.Icon} = 'local_atm' AND ${PlaceSchema.Columns.Icon} = 'currency_exchange'"
        } else {
            return emptyList()
        }

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
                ${PlaceSchema.Columns.Lat} > ?1
                AND ${PlaceSchema.Columns.Lat} < ?2
                AND ${PlaceSchema.Columns.Lon} > ?3
                AND ${PlaceSchema.Columns.Lon} < ?4 
                $where
            ORDER BY ${PlaceSchema.Columns.Lat} DESC
        """

        val cursor = db.rawQuery(
            sql,
            arrayOf(minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString())
        )

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

    fun selectMaxUpdatedAt(db: SQLiteDatabase): ZonedDateTime? {
        db.rawQuery("SELECT max(${PlaceSchema.Columns.UpdatedAt}) FROM ${PlaceSchema.NAME}", null)
            .use {
                it.moveToFirst()
                return if (it.isNull(0)) null else ZonedDateTime.parse(it.getString(0))
            }
    }

    fun selectCount(db: SQLiteDatabase): Long {
        db.rawQuery("SELECT count(*) FROM ${PlaceSchema.NAME}", null).use {
            it.moveToFirst()
            return it.getLong(0)
        }
    }

    fun deleteById(id: Long, db: SQLiteDatabase): Int {
        return db.delete(
            PlaceSchema.NAME,
            "${PlaceSchema.Columns.Id.sqlName} = ?1",
            arrayOf(id.toString()),
        )
    }
}