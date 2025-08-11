package user

import androidx.sqlite.SQLiteConnection
import db.getHttpUrlOrNull
import db.getJsonObjectOld
import db.getZonedDateTime
import db.getZonedDateTimeOrNull
import db.transaction
import java.time.ZonedDateTime
import java.util.regex.Pattern

data class UserQueries(private val conn: SQLiteConnection) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE user (
                id INTEGER NOT NULL PRIMARY KEY,
                osm_data TEXT NOT NULL,
                tags TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """
    }

    fun insertOrReplace(users: List<User>) {
        conn.transaction { conn ->
            users.forEach { user ->
                conn.prepare(
                    """
                    INSERT OR REPLACE
                    INTO user (
                        id,
                        osm_data,
                        tags,
                        updated_at
                    ) VALUES (?1, ?2, ?3, ?4)
                    """
                ).use {
                    user.apply {
                        it.bindLong(1, id)
                        it.bindText(2, osmData.toString())
                        it.bindText(3, tags.toString())
                        it.bindText(4, updatedAt.toString())
                    }

                    it.step()
                }
            }
        }
    }

    fun selectAll(): List<UserListItem> {
        return conn.prepare(
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
        ).use {
            buildList {
                while (it.step()) {
                    add(
                        UserListItem(
                            id = it.getLong(0),
                            image = it.getHttpUrlOrNull(1),
                            name = it.getText(2),
                            tips = getLnUrl(it.getText(3)),
                            changes = it.getLong(4),
                        )
                    )
                }
            }
        }
    }

    fun selectById(id: Long): User? {
        return conn.prepare(
            """
                SELECT
                    id,
                    osm_data,
                    tags,
                    updated_at
                FROM user
                WHERE id = ?1
                """
        ).use {
            it.bindLong(1, id)

            if (it.step()) {
                User(
                    id = it.getLong(0),
                    osmData = it.getJsonObjectOld(1),
                    tags = it.getJsonObjectOld(2),
                    updatedAt = it.getZonedDateTime(3),
                )
            } else {
                null
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        return conn.prepare("SELECT max(updated_at) FROM user").use {
            if (it.step()) {
                it.getZonedDateTimeOrNull(0)
            } else {
                null
            }
        }
    }

    fun selectCount(): Long {
        return conn.prepare("SELECT count(*) FROM user").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM user WHERE id = ?1").use {
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
