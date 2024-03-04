package event

import androidx.core.database.getStringOrNull
import androidx.sqlite.db.transaction
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.util.regex.Pattern

class EventQueries(private val db: SQLiteOpenHelper) {

    fun insertOrReplace(events: List<Event>) {
        db.writableDatabase.transaction {
            events.forEach {
                execSQL(
                    """
                    INSERT OR REPLACE
                    INTO event(
                        id,
                        type,
                        element_id,
                        user_id,
                        tags,
                        created_at,
                        updated_at,
                        deleted_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                    """,
                    arrayOf(
                        it.id,
                        it.type,
                        it.elementId,
                        it.userId,
                        it.tags,
                        it.createdAt,
                        it.updatedAt,
                        it.deletedAt ?: "",
                    ),
                )
            }
        }
    }

    suspend fun selectAll(limit: Long): List<EventListItem> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    ev.type AS event_type,
                    el.id AS element_id,
                    ev.element_id AS osm_id,
                    json_extract(el.osm_json, '$.tags.name') AS element_name,
                    ev.created_at AS event_date,
                    json_extract(u.osm_json, '$.display_name') AS user_name,
                    json_extract(u.osm_json, '$.description') AS user_description
                FROM event ev
                LEFT JOIN element el ON el.osm_id = ev.element_id
                JOIN user u ON u.id = ev.user_id
                WHERE ev.deleted_at == ''
                ORDER BY ev.created_at DESC
                LIMIT ?;
                """,
                arrayOf(limit),
            )

            buildList {
                while (cursor.moveToNext()) {
                    this += EventListItem(
                        eventType = cursor.getString(0),
                        elementId = cursor.getLong(1),
                        osmId = cursor.getStringOrNull(2) ?: "",
                        elementName = cursor.getStringOrNull(3) ?: "",
                        eventDate = cursor.getZonedDateTime(4)!!,
                        userName = cursor.getString(5),
                        userTips = getLnUrl(cursor.getString(6)),
                    )
                }
            }
        }
    }

    suspend fun selectByUserId(userId: Long): List<EventListItem> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    ev.type AS event_type,
                    el.id AS element_id,
                    ev.element_id AS osm_id,
                    json_extract(el.osm_json, '$.tags.name') AS element_name,
                    ev.created_at AS event_date
                FROM event ev
                LEFT JOIN element el ON el.osm_id = ev.element_id
                JOIN user u ON u.id = ev.user_id
                WHERE ev.user_id = ?
                ORDER BY ev.created_at DESC;
                """,
                arrayOf(userId),
            )

            buildList {
                while (cursor.moveToNext()) {
                    this += EventListItem(
                        eventType = cursor.getString(0),
                        elementId = cursor.getLong(1),
                        osmId = cursor.getStringOrNull(2) ?: "",
                        elementName = cursor.getStringOrNull(3) ?: "",
                        eventDate = cursor.getZonedDateTime(4)!!,
                        userName = "",
                        userTips = "",
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
                FROM event;
                """
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            cursor.getZonedDateTime(0)
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