package element_comment

import androidx.sqlite.SQLiteConnection
import conn
import getZonedDateTimeOrNull
import transaction
import java.time.ZonedDateTime

class ElementCommentQueries {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE element_comment (
                id INTEGER NOT NULL PRIMARY KEY,
                element_id INTEGER NOT NULL,
                comment TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """
        const val CREATE_INDICES = """
            CREATE INDEX IF NOT EXISTS element_comment_element_id ON element_comment(element_id);
            """
    }

    fun insertOrReplace(comments: List<ElementComment>, conn: SQLiteConnection = conn()) {
        val sql = """
            INSERT OR REPLACE
            INTO element_comment (
                id,
                element_id,
                comment,
                created_at,
                updated_at
            ) VALUES (?1, ?2, ?3, ?4, ?5)
        """

        conn.transaction {
            val stmt = conn.prepare(sql)

            comments.forEach { comment ->
                stmt.reset()
                stmt.clearBindings()

                stmt.bindLong(1, comment.id)
                stmt.bindLong(2, comment.elementId)
                stmt.bindText(3, comment.comment)
                stmt.bindText(4, comment.createdAt)
                stmt.bindText(5, comment.updatedAt)

                stmt.step()
            }
        }
    }

    fun selectById(id: Long, conn: SQLiteConnection = conn()): ElementComment? {
        return conn.prepare(
            """
                SELECT
                    id,
                    element_id,
                    comment,
                    created_at,
                    updated_at
                FROM element_comment
                WHERE id = ?1
                """
        ).use {
            it.bindLong(1, id)

            if (it.step()) {
                ElementComment(
                    id = it.getLong(0),
                    elementId = it.getLong(1),
                    comment = it.getText(2),
                    createdAt = it.getText(3),
                    updatedAt = it.getText(4),
                )
            } else {
                null
            }
        }
    }

    fun selectByElementId(elementId: Long, conn: SQLiteConnection = conn()): List<ElementComment> {
        return conn.prepare(
            """
                SELECT
                    id,
                    element_id,
                    comment,
                    created_at,
                    updated_at
                FROM element_comment
                WHERE element_id = ?1
                ORDER BY created_at DESC
                """
        ).use {
            it.bindLong(1, elementId)

            buildList {
                while (it.step()) {
                    add(
                        ElementComment(
                            id = it.getLong(0),
                            elementId = it.getLong(1),
                            comment = it.getText(2),
                            createdAt = it.getText(3),
                            updatedAt = it.getText(4),
                        )
                    )
                }
            }
        }
    }

    fun selectMaxUpdatedAt(conn: SQLiteConnection = conn()): ZonedDateTime? {
        return conn.prepare("SELECT max(updated_at) FROM element_comment").use {
            if (it.step()) {
                it.getZonedDateTimeOrNull(0)
            } else {
                null
            }
        }
    }

    fun selectCount(conn: SQLiteConnection = conn()): Long {
        return conn.prepare("SELECT count(*) FROM element_comment").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long, conn: SQLiteConnection = conn()) {
        conn.prepare("DELETE FROM element_comment WHERE id = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }
}