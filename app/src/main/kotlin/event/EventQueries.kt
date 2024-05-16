package event

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import db.getJsonObject
import db.getText
import db.getZonedDateTime
import java.time.ZonedDateTime
import java.util.regex.Pattern

class EventQueries(private val conn: SQLiteConnection) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE event (
                id INTEGER NOT NULL PRIMARY KEY,
                user_id INTEGER NOT NULL,
                element_id INTEGER NOT NULL,
                type INTEGER NOT NULL,
                tags TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """
    }

    fun insertOrReplace(events: List<Event>) {
        conn.execSQL("BEGIN IMMEDIATE TRANSACTION")

        try {
            events.forEach { insertOrReplace(it) }
            conn.execSQL("END TRANSACTION")
        } catch (t: Throwable) {
            conn.execSQL("ROLLBACK TRANSACTION")
        }
    }

    fun insertOrReplace(event: Event) {
        conn.prepare(
            """
            INSERT OR REPLACE
            INTO event(
                id,
                user_id,
                element_id,
                type,
                tags,
                created_at,
                updated_at
            ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
            """
        ).use {
            event.apply {
                it.bindLong(1, id)
                it.bindLong(2, userId)
                it.bindLong(3, elementId)
                it.bindLong(4, type)
                it.bindText(5, tags.toString())
                it.bindText(6, createdAt.toString())
                it.bindText(7, updatedAt.toString())
            }

            it.step()
        }
    }

    fun selectById(id: Long): Event? {
        return conn.prepare(
            """
            SELECT
                id,
                user_id,
                element_id,
                type,
                tags,
                created_at,
                updated_at
            FROM event
            WHERE id = ?1
            """
        ).use {
            it.bindLong(1, id)

            if (it.step()) {
                Event(
                    id = it.getLong(0),
                    userId = it.getLong(1),
                    elementId = it.getLong(2),
                    type = it.getLong(3),
                    tags = it.getJsonObject(4),
                    createdAt = it.getZonedDateTime(5),
                    updatedAt = it.getZonedDateTime(6),
                )
            } else {
                null
            }
        }
    }

    fun selectAll(limit: Long): List<EventListItem> {
        return conn.prepare(
            """
            SELECT
                ev.type AS event_type,
                el.id AS element_id,
                json_extract(el.overpass_data, '$.tags.name') AS element_name,
                ev.created_at AS event_date,
                json_extract(u.osm_data, '$.display_name') AS user_name,
                json_extract(u.osm_data, '$.description') AS user_description
            FROM event ev
            LEFT JOIN element el ON el.id = ev.element_id
            JOIN user u ON u.id = ev.user_id
            ORDER BY ev.created_at DESC
            LIMIT ?1
            """
        ).use {
            it.bindLong(1, limit)

            buildList {
                while (it.step()) {
                    add(
                        EventListItem(
                            eventType = it.getLong(0),
                            elementId = it.getLong(1),
                            elementName = it.getText(2, ""),
                            eventDate = it.getZonedDateTime(3),
                            userName = it.getText(4),
                            userTips = getLnUrl(it.getText(5)),
                        )
                    )
                }
            }
        }
    }

    fun selectByUserId(userId: Long): List<EventListItem> {
        return conn.prepare(
            """
            SELECT
                ev.type AS event_type,
                el.id AS element_id,
                json_extract(el.overpass_data, '$.tags.name') AS element_name,
                ev.created_at AS event_date
            FROM event ev
            LEFT JOIN element el ON el.id = ev.element_id
            JOIN user u ON u.id = ev.user_id
            WHERE ev.user_id = ?1
            ORDER BY ev.created_at DESC
            """
        ).use {
            it.bindLong(1, userId)

            buildList {
                while (it.step()) {
                    add(
                        EventListItem(
                            eventType = it.getLong(0),
                            elementId = it.getLong(1),
                            elementName = it.getText(2, ""),
                            eventDate = it.getZonedDateTime(3),
                            userName = "",
                            userTips = "",
                        )
                    )
                }
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        return conn.prepare("SELECT max(updated_at) FROM event").use {
            if (it.step()) {
                it.getZonedDateTime(0)
            } else {
                null
            }
        }
    }

    fun selectCount(): Long {
        return conn.prepare("SELECT count(*) FROM event").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM event WHERE id = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }

    private fun getLnUrl(description: String): String {
        val pattern = Pattern.compile("\\(lightning:[^)]*\\)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(description)
        val matchFound: Boolean = matcher.find()

        return if (matchFound) {
            matcher.group().trim('(', ')')
        } else {
            ""
        }
    }
}