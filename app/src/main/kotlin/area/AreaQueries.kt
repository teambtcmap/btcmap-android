package area

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.use
import db.getJsonObject
import db.getZonedDateTime
import java.time.ZonedDateTime

class AreaQueries(private val conn: SQLiteConnection) {

    fun insertOrReplace(area: Area) {
        conn.prepare("INSERT OR REPLACE INTO area(id, tags, updated_at) VALUES(?1, ?2, ?3)").use {
            it.bindLong(1, area.id)
            it.bindText(2, area.tags.toString())
            it.bindText(3, area.updatedAt.toString())
            it.step()
        }
    }

    fun selectById(id: Long): Area? {
        conn.prepare("SELECT id, tags, updated_at FROM area WHERE id = ?1").use {
            it.bindLong(1, id)

            return if (it.step()) {
                Area(
                    id = it.getLong(0),
                    tags = it.getJsonObject(1),
                    updatedAt = it.getZonedDateTime(2),
                )
            } else {
                null
            }
        }
    }

    fun selectByType(type: String): List<Area> {
        val sql = """
            SELECT id, tags, updated_at
            FROM area
            WHERE json_extract(tags, '$.type') = ?1
            """

        val rows = mutableListOf<Area>()

        conn.prepare(sql).use {
            it.bindText(1, type)

            while (it.step()) {
                rows += Area(
                    id = it.getLong(0),
                    tags = it.getJsonObject(1),
                    updatedAt = it.getZonedDateTime(2),
                )
            }
        }

        return rows
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        conn.prepare("SELECT max(updated_at) FROM area").use {
            return if (it.step()) {
                it.getZonedDateTime(0)
            } else {
                null
            }
        }
    }

    fun selectMeetups(): List<Meetup> {
        val sql = """
            SELECT
                json_extract(tags, '$.meetup_lat') AS lat,
                json_extract(tags, '$.meetup_lon') AS lon,
                id
            FROM area
            WHERE 
                lat IS NOT NULL 
                AND lon IS NOT NULL
            """

        val rows = mutableListOf<Meetup>()

        conn.prepare(sql).use {
            while (it.step()) {
                rows += Meetup(
                    lat = it.getDouble(0),
                    lon = it.getDouble(1),
                    areaId = it.getLong(2),
                )
            }
        }

        return rows
    }

    fun selectCount(): Long {
        return conn.prepare("SELECT count(*) FROM area").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM area WHERE id = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }
}