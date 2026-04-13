package org.btcmap.db.table

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.google.gson.JsonArray
import org.btcmap.db.getJsonArray

object UserSchema {
    const val TABLE_NAME = "user"

    override fun toString(): String {
        return """
            CREATE TABLE $TABLE_NAME (
                ${Columns.Id} INTEGER PRIMARY KEY NOT NULL,
                ${Columns.Name} TEXT NOT NULL,
                ${Columns.Roles} TEXT NOT NULL DEFAULT '[]',
                ${Columns.SavedPlaces} TEXT NOT NULL DEFAULT '[]',
                ${Columns.SavedAreas} TEXT NOT NULL DEFAULT '[]'
            );
        """.trimIndent()
    }

    enum class Columns(val sqlName: String) {
        Id("id"),
        Name("name"),
        Roles("roles"),
        SavedPlaces("saved_places"),
        SavedAreas("saved_areas");

        override fun toString() = sqlName
    }
}

typealias User = UserProjectionFull

data class UserProjectionFull(
    val id: Long,
    val name: String,
    val roles: JsonArray,
    val savedPlaces: JsonArray,
    val savedAreas: JsonArray,
) {
    companion object {
        val columns: String
            get() {
                return UserSchema.Columns.entries.joinToString(",") { it.sqlName }
            }

        fun fromStatement(stmt: SQLiteStatement): UserProjectionFull {
            return UserProjectionFull(
                id = stmt.getLong(0),
                name = stmt.getText(1),
                roles = stmt.getJsonArray(2),
                savedPlaces = stmt.getJsonArray(3),
                savedAreas = stmt.getJsonArray(4),
            )
        }
    }
}

class UserQueries(private val conn: SQLiteConnection) {
    fun insert(row: User) {
        val sql = """
            INSERT INTO ${UserSchema.TABLE_NAME} (${User.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5);
        """

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, row.id)
            stmt.bindText(2, row.name)
            stmt.bindText(3, row.roles.toString())
            stmt.bindText(4, row.savedPlaces.toString())
            stmt.bindText(5, row.savedAreas.toString())
            stmt.step()
        }
    }

    fun select(): User? {
        return conn.prepare(
            """
                SELECT ${UserProjectionFull.columns}
                FROM ${UserSchema.TABLE_NAME};
            """
        ).use {
            if (it.step()) {
                UserProjectionFull.fromStatement(it)
            } else {
                null
            }
        }
    }

    fun delete() {
        conn.prepare("DELETE FROM ${UserSchema.TABLE_NAME};").use { it.step() }
    }
}