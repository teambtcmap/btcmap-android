package user

import db.getHttpUrl
import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import java.time.ZonedDateTime
import java.util.regex.Pattern

data class UserQueries(val db: SQLiteOpenHelper) {

    fun insertOrReplace(user: User) {
        db.writableDatabase.execSQL(
            """
            INSERT OR REPLACE
            INTO user (
                id,
                osm_data,
                tags,
                updated_at
            ) VALUES (?, ?, ?, ?)
            """,
            arrayOf(
                user.id,
                user.osmData,
                user.tags,
                user.updatedAt,
            ),
        )
    }

    fun selectAll(): List<UserListItem> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                u.id AS id,
                json_extract(u.osm_data, '$.img.href') AS image,
                json_extract(u.osm_data, '$.display_name') AS name,
                json_extract(u.osm_data, '$.description') AS description,
                count(e.user_id) AS changes
            FROM user u
            LEFT JOIN event e ON e.user_id = u.id AND json_extract(e.tags, '$.automated') IS NULL
            GROUP BY u.id
            ORDER BY changes DESC
            """
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    UserListItem(
                        id = cursor.getLong(0),
                        image = cursor.getHttpUrl(1),
                        name = cursor.getString(2),
                        tips = getLnUrl(cursor.getString(3)),
                        changes = cursor.getLong(4),
                    )
                )
            }
        }
    }

    fun selectById(id: Long): User? {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                id,
                osm_data,
                tags,
                updated_at
            FROM user
            WHERE id = ?
            """,
            arrayOf(id),
        )

        if (!cursor.moveToNext()) {
            return null
        }

        return User(
            id = cursor.getLong(0),
            osmData = cursor.getJsonObject(1),
            tags = cursor.getJsonObject(2),
            updatedAt = cursor.getZonedDateTime(3)!!,
        )
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        val cursor = db.readableDatabase.query("SELECT max(updated_at) FROM user")

        if (!cursor.moveToNext()) {
            return null
        }

        return cursor.getZonedDateTime(0)
    }

    fun selectCount(): Long {
        val cursor = db.readableDatabase.query("SELECT count(*) FROM user")
        cursor.moveToNext()
        return cursor.getLong(0)
    }

    fun deleteById(id: Long) {
        db.readableDatabase.query(
            "DELETE FROM user WHERE id = ?",
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
