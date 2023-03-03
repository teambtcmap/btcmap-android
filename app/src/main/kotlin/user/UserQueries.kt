package user

import androidx.sqlite.db.transaction
import db.getHttpUrl
import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.util.regex.Pattern

data class UserQueries(private val db: SQLiteOpenHelper) {

    suspend fun insertOrReplace(users: List<User>) {
        withContext(Dispatchers.IO) {
            db.writableDatabase.transaction {
                users.forEach {
                    execSQL(
                        """
                        INSERT OR REPLACE
                        INTO user (
                            id,
                            osm_json,
                            tags,
                            created_at,
                            updated_at,
                            deleted_at
                        ) VALUES (?, ?, ?, ?, ?, ?);
                        """,
                        arrayOf(
                            it.id,
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

    suspend fun selectAll(): List<UserListItem> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    u.id AS id,
                    json_extract(u.osm_json, '$.img.href') AS image,
                    json_extract(u.osm_json, '$.display_name') AS name,
                    json_extract(u.osm_json, '$.description') AS description,
                    count(e.user_id) AS changes
                FROM user u
                LEFT JOIN event e ON e.user_id = u.id AND e.deleted_at = ''
                GROUP BY u.id
                ORDER BY changes DESC;
                """
            )

            buildList {
                while (cursor.moveToNext()) {
                    this += UserListItem(
                        id = cursor.getLong(0),
                        image = cursor.getHttpUrl(1),
                        name = cursor.getString(2),
                        tips = getLnUrl(cursor.getString(3)),
                        changes = cursor.getLong(4),
                    )
                }
            }
        }
    }

    suspend fun selectById(id: Long): User? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    id,
                    osm_json,
                    tags,
                    created_at,
                    updated_at,
                    deleted_at
                FROM user
                WHERE id = ?;
                """,
                arrayOf(id),
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            User(
                id = cursor.getLong(0),
                osmJson = cursor.getJsonObject(1),
                tags = cursor.getJsonObject(2),
                createdAt = cursor.getZonedDateTime(3)!!,
                updatedAt = cursor.getZonedDateTime(4)!!,
                deletedAt = cursor.getZonedDateTime(5),
            )
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

    suspend fun selectMaxUpdatedAt(): ZonedDateTime? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT max(updated_at)
                FROM user;
                """
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            cursor.getZonedDateTime(0)
        }
    }
}
