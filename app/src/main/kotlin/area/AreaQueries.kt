package area

import androidx.sqlite.db.transaction
import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class AreaQueries(private val db: SQLiteOpenHelper) {

    suspend fun insertOrReplace(areas: List<Area>) {
        withContext(Dispatchers.IO) {
            db.writableDatabase.transaction {
                areas.forEach {
                    execSQL(
                        """
                        INSERT OR REPLACE
                        INTO area(
                            id,
                            tags,
                            created_at,
                            updated_at,
                            deleted_at
                        )
                        VALUES(
                            ?,
                            ?,
                            ?,
                            ?,
                            ?
                        );
                        """,
                        arrayOf(
                            it.id,
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

    suspend fun selectById(id: String): Area? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    id,
                    tags,
                    created_at,
                    updated_at,
                    deleted_at
                FROM area
                WHERE id = ?;
                """,
                arrayOf(id),
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            Area(
                id = cursor.getString(0),
                tags = cursor.getJsonObject(1),
                createdAt = cursor.getZonedDateTime(2)!!,
                updatedAt = cursor.getZonedDateTime(3)!!,
                deletedAt = cursor.getZonedDateTime(4),
            )
        }
    }

    suspend fun selectByType(type: String): List<Area> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    id,
                    tags,
                    created_at,
                    updated_at,
                    deleted_at
                FROM area
                WHERE 
                    json_extract(tags, '$.type') = '$type'
                    AND deleted_at = '';
                """
            )

            val rows = mutableListOf<Area>()

            while (cursor.moveToNext()) {
                rows += Area(
                    id = cursor.getString(0),
                    tags = cursor.getJsonObject(1),
                    createdAt = cursor.getZonedDateTime(2)!!,
                    updatedAt = cursor.getZonedDateTime(3)!!,
                    deletedAt = cursor.getZonedDateTime(4),
                )
            }

            rows
        }
    }

    suspend fun selectMaxUpdatedAt(): ZonedDateTime? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT max(updated_at)
                FROM area;
                """
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            cursor.getZonedDateTime(0)
        }

    }

    suspend fun selectMeetups(): List<Meetup> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    json_extract(tags, '$.meetup_lat') AS lat,
                    json_extract(tags, '$.meetup_lon') AS lon,
                    id
                FROM area
                WHERE 
                    lat IS NOT NULL AND lon IS NOT NULL
                """
            )

            val rows = mutableListOf<Meetup>()

            while (cursor.moveToNext()) {
                rows += Meetup(
                    lat = cursor.getDouble(0),
                    lon = cursor.getDouble(1),
                    areaId = cursor.getString(2),
                )
            }

            rows
        }
    }
}