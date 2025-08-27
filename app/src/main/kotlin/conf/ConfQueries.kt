package conf

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import db.transaction

class ConfQueries(private val conn: SQLiteConnection) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE conf (
                show_atms INTEGER NOT NULL,
                map_style TEXT NOT NULL
            );
            """
    }

    fun insertOrReplace(conf: Conf) {
        conn.transaction { conn ->
            conn.execSQL("DELETE FROM conf")

            conn.prepare(
                """   
                INSERT
                INTO conf (
                    show_atms,
                    map_style
                )
                VALUES (?1, ?2);
                """
            ).use {
                it.bindLong(1, if (conf.showAtms) 1 else 0)
                it.bindText(2, conf.mapStyle.toString())
                it.step()
            }
        }
    }

    fun select(): Conf? {
        return conn.prepare(
            """
                SELECT
                    show_atms,
                    map_style
                FROM conf
                """
        ).use {
            if (it.step()) {
                Conf(
                    showAtms = it.getBoolean(0),
                    mapStyle = MapStyle.valueOf(it.getText(1))
                )
            } else {
                null
            }
        }
    }
}