package area

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.use
import db.getInstant
import db.getJsonObject
import db.getZonedDateTime
import db.getZonedDateTimeOrNull
import db.transaction
import java.time.ZonedDateTime

class AreaQueries(private val conn: SQLiteConnection) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE area (
                id INTEGER NOT NULL PRIMARY KEY,
                tags TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """
    }

    fun insertOrReplace(areas: List<Area>) {
        conn.transaction { conn ->
            areas.forEach { area ->
                conn.prepare("INSERT OR REPLACE INTO area(id, tags, updated_at) VALUES(?1, ?2, ?3)")
                    .use {
                        it.bindLong(1, area.id)
                        it.bindText(2, area.tags.toString())
                        it.bindText(3, area.updatedAt.toString())
                        it.step()
                    }
            }
        }
    }

    fun selectById(id: Long): Area? {
        return conn.prepare("SELECT id, tags, updated_at FROM area WHERE id = ?1").use {
            it.bindLong(1, id)

            if (it.step()) {
                Area(
                    id = it.getLong(0),
                    tags = it.getJsonObject(1),
                    updatedAt = it.getInstant(2),
                )
            } else {
                null
            }
        }
    }

    fun selectByType(type: String): List<Area> {
        return conn.prepare(
            """
                SELECT id, tags, updated_at
                FROM area
                WHERE json_extract(tags, '$.type') = ?1
                """
        ).use {
            it.bindText(1, type)

            buildList {
                while (it.step()) {
                    add(
                        Area(
                            id = it.getLong(0),
                            tags = it.getJsonObject(1),
                            updatedAt = it.getInstant(2),
                        )
                    )
                }
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        return conn.prepare("SELECT max(updated_at) FROM area").use {
            if (it.step()) {
                it.getZonedDateTimeOrNull(0)
            } else {
                null
            }
        }
    }

    fun selectMeetups(): List<Meetup> {
        return conn.prepare(
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
        ).use {
            buildList {
                while (it.step()) {
                    add(
                        Meetup(
                            lat = it.getDouble(0),
                            lon = it.getDouble(1),
                            areaId = it.getLong(2),
                        )
                    )
                }
            }
        }
    }

    fun selectCount(): Long {
        return conn.prepare("SELECT count(*) FROM area").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        return conn.prepare("DELETE FROM area WHERE id = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }
}