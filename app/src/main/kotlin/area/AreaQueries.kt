package area

import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import java.time.ZonedDateTime

class AreaQueries(val db: SQLiteOpenHelper) {

    fun insertOrReplace(area: Area) {
        db.writableDatabase.execSQL(
            """
            INSERT OR REPLACE
            INTO area(id, tags, updated_at)
            VALUES(?, ?, ?)
            """,
            arrayOf(
                area.id,
                area.tags,
                area.updatedAt,
            ),
        )
    }

    fun selectById(id: Long): Area? {
        val cursor = db.readableDatabase.query(
            """
            SELECT id, tags, updated_at
            FROM area
            WHERE id = ?;
            """,
            arrayOf(id),
        )

        if (!cursor.moveToNext()) {
            return null
        }

        return Area(
            id = cursor.getLong(0),
            tags = cursor.getJsonObject(1),
            updatedAt = cursor.getZonedDateTime(2)!!,
        )
    }

    fun selectByType(type: String): List<Area> {
        val cursor = db.readableDatabase.query(
            """
            SELECT id, tags, updated_at
            FROM area
            WHERE json_extract(tags, '$.type') = ?
            """,
            arrayOf(type),
        )

        val rows = mutableListOf<Area>()

        while (cursor.moveToNext()) {
            rows += Area(
                id = cursor.getLong(0),
                tags = cursor.getJsonObject(1),
                updatedAt = cursor.getZonedDateTime(2)!!,
            )
        }

        return rows
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        val cursor = db.readableDatabase.query("SELECT max(updated_at) FROM area")

        if (!cursor.moveToNext()) {
            return null
        }

        return cursor.getZonedDateTime(0)
    }

    fun selectMeetups(): List<Meetup> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                json_extract(tags, '$.meetup_lat') AS lat,
                json_extract(tags, '$.meetup_lon') AS lon,
                id
            FROM area
            WHERE 
                lat IS NOT NULL 
                AND lon IS NOT NULL
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

        return rows
    }

    fun selectCount(): Long {
        val cursor = db.readableDatabase.query("SELECT count(*) FROM area")
        cursor.moveToNext()
        return cursor.getLong(0)
    }

    fun deleteById(id: Long) {
        db.readableDatabase.query(
            "DELETE FROM area WHERE id = ?",
            arrayOf(id),
        )
    }
}