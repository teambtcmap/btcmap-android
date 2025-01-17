package element_comment

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.use
import db.getZonedDateTime
import db.getZonedDateTimeOrNull
import db.transaction
import java.time.ZonedDateTime

class ElementCommentQueries(private val conn: SQLiteConnection) {

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

    fun insertOrReplace(comments: List<ElementComment>) {
        conn.transaction { conn ->
            comments.forEach { comment ->
                conn.prepare(
                    """
                    INSERT OR REPLACE
                    INTO element_comment (
                        id,
                        element_id,
                        comment,
                        created_at,
                        updated_at
                    ) VALUES (?1, ?2, ?3, ?4, ?5)
                    """
                ).use {
                    it.bindLong(1, comment.id)
                    it.bindLong(2, comment.elementId)
                    it.bindText(3, comment.comment)
                    it.bindText(4, comment.createdAt)
                    it.bindText(5, comment.updatedAt)
                    it.step()
                }
            }
        }
    }

    fun selectById(id: Long): ElementComment? {
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

    fun selectByElementId(elementId: Long): List<ElementComment> {
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

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        return conn.prepare("SELECT max(updated_at) FROM element_comment").use {
            if (it.step()) {
                it.getZonedDateTimeOrNull(0)
            } else {
                null
            }
        }
    }

    fun selectCount(): Long {
        return conn.prepare("SELECT count(*) FROM element_comment").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM element_comment WHERE id = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }
}