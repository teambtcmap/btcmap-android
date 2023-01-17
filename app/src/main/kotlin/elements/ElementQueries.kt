package elements

import androidx.sqlite.db.transaction
import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class ElementQueries(private val db: SQLiteOpenHelper) {

    suspend fun insertOrReplace(elements: List<Element>) {
        withContext(Dispatchers.IO) {
            db.writableDatabase.transaction {
                elements.forEach {
                    execSQL(
                        """
                        INSERT OR REPLACE
                        INTO element (
                            id,
                            lat,
                            lon,
                            osm_json,
                            tags,
                            created_at,
                            updated_at,
                            deleted_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                        """,
                        arrayOf(
                            it.id,
                            it.lat,
                            it.lon,
                            it.osmJson,
                            it.tags,
                            it.createdAt,
                            it.updatedAt,
                            it.deletedAt ?: "",
                        ),
                    )
                }
            }
        }
    }

    suspend fun selectById(id: String): Element? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    id,
                    lat,
                    lon,
                    osm_json,
                    tags,
                    created_at,
                    updated_at,
                    deleted_at
                FROM element
                WHERE id = ?;
                """,
                arrayOf(id),
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            Element(
                id = cursor.getString(0),
                lat = cursor.getDouble(1),
                lon = cursor.getDouble(2),
                osmJson = cursor.getJsonObject(3),
                tags = cursor.getJsonObject(4),
                createdAt = cursor.getZonedDateTime(5)!!,
                updatedAt = cursor.getZonedDateTime(6)!!,
                deletedAt = cursor.getZonedDateTime(7),
            )
        }
    }

    suspend fun selectBySearchString(searchString: String): List<Element> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT *
                FROM element
                WHERE deleted_at = '' AND (UPPER(osm_json) LIKE '%' || UPPER(?) || '%')
                LIMIT 100;
                """,
                arrayOf(searchString),
            )

            buildList {
                while (cursor.moveToNext()) {
                    this += Element(
                        id = cursor.getString(0),
                        lat = cursor.getDouble(1),
                        lon = cursor.getDouble(2),
                        osmJson = cursor.getJsonObject(3),
                        tags = cursor.getJsonObject(4),
                        createdAt = cursor.getZonedDateTime(5)!!,
                        updatedAt = cursor.getZonedDateTime(6)!!,
                        deletedAt = cursor.getZonedDateTime(7),
                    )
                }
            }
        }
    }

    suspend fun selectWithoutClustering(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<ElementsCluster> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    id,
                    lat,
                    lon,
                    json_extract(tags, '$.icon:android') AS icon_id,
                    json_extract(tags, '$.boost:expires') AS boost_expires
                FROM element
                WHERE
                    deleted_at = ''
                    AND lat > ?
                    AND lat < ?
                    AND lon > ?
                    AND lon < ?
                ORDER BY lat DESC;
                """,
                arrayOf(
                    minLat,
                    maxLat,
                    minLon,
                    maxLon,
                ),
            )

            buildList {
                while (cursor.moveToNext()) {
                    this += ElementsCluster(
                        count = 1,
                        id = cursor.getString(0),
                        lat = cursor.getDouble(1),
                        lon = cursor.getDouble(2),
                        iconId = cursor.getString(3),
                        boostExpires = cursor.getZonedDateTime(4),
                    )
                }
            }
        }
    }

    suspend fun selectClusters(step: Double): List<ElementsCluster> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    count(*),
                    e.id,
                    avg(e.lat) AS lat,
                    avg(e.lon) AS lon,
                    json_extract(e.tags, '$.icon:android') AS icon_id,
                    json_extract(e.tags, '$.boost:expires') AS boost_expires
                FROM element e
                WHERE e.deleted_at = ''
                GROUP BY round(lat / ?) * ?, round(lon / ?) * ?
                ORDER BY e.lat DESC;
                """,
                arrayOf(step, step, step, step),
            )

            buildList {
                while (cursor.moveToNext()) {
                    this += ElementsCluster(
                        count = cursor.getLong(0),
                        id = cursor.getString(1),
                        lat = cursor.getDouble(2),
                        lon = cursor.getDouble(3),
                        iconId = cursor.getString(4),
                        boostExpires = cursor.getZonedDateTime(5),
                    )
                }
            }
        }
    }

    suspend fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<AreaElement> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    id,
                    json_extract(tags, '$.icon:android') AS icon_id,
                    json_extract(osm_json, '$.tags') AS osm_tags
                FROM element
                WHERE
                deleted_at = ''
                AND lat > ?
                AND lat < ?
                AND lon > ?
                AND lon < ?;
                """,
                arrayOf(
                    minLat,
                    maxLat,
                    minLon,
                    maxLon,
                ),
            )

            buildList {
                while (cursor.moveToNext()) {
                    this += AreaElement(
                        id = cursor.getString(0),
                        icon = cursor.getString(1),
                        osmTags = cursor.getJsonObject(2),
                    )
                }
            }
        }
    }

    suspend fun selectMaxUpdatedAt(): ZonedDateTime? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT max(updated_at)
                FROM element;
                """
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            cursor.getZonedDateTime(0)
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT count(*)
                FROM element;
                """
            )

            cursor.moveToNext()
            cursor.getLong(0)
        }
    }
}