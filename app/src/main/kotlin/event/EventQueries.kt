package event

import androidx.core.database.getStringOrNull
import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import java.time.ZonedDateTime
import java.util.regex.Pattern

class EventQueries(val db: SQLiteOpenHelper) {

    fun insertOrReplace(event: Event) {
        db.writableDatabase.execSQL(
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
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            arrayOf(
                event.id,
                event.userId,
                event.elementId,
                event.type,
                event.tags,
                event.createdAt,
                event.updatedAt,
            ),
        )
    }

    fun selectById(id: Long): Event? {
        val cursor = db.readableDatabase.query(
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
            WHERE id = ?
            """,
            arrayOf(id),
        )

        if (!cursor.moveToNext()) {
            return null
        }

        return Event(
            id = cursor.getLong(0),
            userId = cursor.getLong(1),
            elementId = cursor.getLong(2),
            type = cursor.getLong(3),
            tags = cursor.getJsonObject(4),
            createdAt = cursor.getZonedDateTime(5)!!,
            updatedAt = cursor.getZonedDateTime(6)!!,
        )
    }

    fun selectAll(limit: Long): List<EventListItem> {
        val cursor = db.readableDatabase.query(
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
            LIMIT ?
            """,
            arrayOf(limit),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    EventListItem(
                        eventType = cursor.getLong(0),
                        elementId = cursor.getLong(1),
                        elementName = cursor.getStringOrNull(2) ?: "",
                        eventDate = cursor.getZonedDateTime(3)!!,
                        userName = cursor.getString(4),
                        userTips = getLnUrl(cursor.getString(5)),
                    )
                )
            }
        }
    }

    fun selectByUserId(userId: Long): List<EventListItem> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                ev.type AS event_type,
                el.id AS element_id,
                json_extract(el.overpass_data, '$.tags.name') AS element_name,
                ev.created_at AS event_date
            FROM event ev
            LEFT JOIN element el ON el.id = ev.element_id
            JOIN user u ON u.id = ev.user_id
            WHERE ev.user_id = ?
            ORDER BY ev.created_at DESC
            """,
            arrayOf(userId),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    EventListItem(
                        eventType = cursor.getLong(0),
                        elementId = cursor.getLong(1),
                        elementName = cursor.getStringOrNull(2) ?: "",
                        eventDate = cursor.getZonedDateTime(3)!!,
                        userName = "",
                        userTips = "",
                    )
                )
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        val cursor = db.readableDatabase.query("SELECT max(updated_at) FROM event")

        if (!cursor.moveToNext()) {
            return null
        }

        return cursor.getZonedDateTime(0)
    }

    fun selectCount(): Long {
        val cursor = db.readableDatabase.query("SELECT count(*) FROM event")
        cursor.moveToNext()
        return cursor.getLong(0)
    }

    fun deleteById(id: Long) {
        db.readableDatabase.query(
            "DELETE FROM event WHERE id = ?",
            arrayOf(id),
        )
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