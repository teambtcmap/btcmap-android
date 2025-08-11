package area_element

import androidx.sqlite.SQLiteConnection
import db.getZonedDateTimeOrNull
import db.transaction
import java.time.ZonedDateTime

class AreaElementQueries(private val conn: SQLiteConnection) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE area_element (
                id INTEGER NOT NULL PRIMARY KEY,
                area_id INTEGER NOT NULL,
                element_id INTEGER NOT NULL,
                updated_at TEXT NOT NULL
            );
            """

        const val CREATE_INDICES = """
            CREATE INDEX IF NOT EXISTS area_element_area_id ON area_element(area_id);
            CREATE INDEX IF NOT EXISTS area_element_element_id ON area_element(element_id);
            """
    }

    fun insertOrReplace(items: List<AreaElement>) {
        conn.transaction { conn ->
            items.forEach { item ->
                conn.prepare(
                    """
                    INSERT OR REPLACE
                    INTO area_element (
                        id,
                        area_id,
                        element_id,
                        updated_at
                    ) VALUES (?1, ?2, ?3, ?4)
                    """
                ).use {
                    it.bindLong(1, item.id)
                    it.bindLong(2, item.areaId)
                    it.bindLong(3, item.elementId)
                    it.bindText(4, item.updatedAt)
                    it.step()
                }
            }
        }
    }

    fun selectById(id: Long): AreaElement? {
        return conn.prepare(
            """
                SELECT
                    id,
                    area_id,
                    element_id,
                    updated_at
                FROM area_element
                WHERE id = ?1
                """
        ).use {
            it.bindLong(1, id)

            if (it.step()) {
                AreaElement(
                    id = it.getLong(0),
                    areaId = it.getLong(1),
                    elementId = it.getLong(2),
                    updatedAt = it.getText(3),
                )
            } else {
                null
            }
        }
    }

    fun selectByAreaId(areaId: Long): List<AreaElement> {
        return conn.prepare(
            """
                SELECT
                    id,
                    area_id,
                    element_id,
                    updated_at
                FROM area_element
                WHERE area_id = ?1
                """
        ).use {
            it.bindLong(1, areaId)

            buildList {
                while (it.step()) {
                    add(
                        AreaElement(
                            id = it.getLong(0),
                            areaId = it.getLong(1),
                            elementId = it.getLong(2),
                            updatedAt = it.getText(3),
                        )
                    )
                }
            }
        }
    }

    fun selectByElementId(elementId: Long): List<AreaElement> {
        return conn.prepare(
            """
                SELECT
                    id,
                    area_id,
                    element_id,
                    updated_at
                FROM area_element
                WHERE element_id = ?1
                """
        ).use {
            it.bindLong(1, elementId)

            buildList {
                while (it.step()) {
                    add(
                        AreaElement(
                            id = it.getLong(0),
                            areaId = it.getLong(1),
                            elementId = it.getLong(2),
                            updatedAt = it.getText(3),
                        )
                    )
                }
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        return conn.prepare("SELECT max(updated_at) FROM area_element").use {
            if (it.step()) {
                it.getZonedDateTimeOrNull(0)
            } else {
                null
            }
        }
    }

    fun selectCount(): Long {
        return conn.prepare("SELECT count(*) FROM area_element").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM area_element WHERE id = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }
}