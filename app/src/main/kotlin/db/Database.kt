package db

import android.content.Context
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime

val elementsUpdatedAt = MutableStateFlow(LocalDateTime.now())

const val DB_FILE_NAME = "btcmap-2024-04-26.db"
const val DB_VERSION = 2

class Database(context: Context) : SQLiteOpenHelper(
    context,
    DB_FILE_NAME,
    null,
    DB_VERSION,
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_CONF_TABLE)
        db.execSQL(CREATE_EVENT_TABLE)
        db.execSQL(CREATE_ELEMENT_TABLE)
        db.execSQL(CREATE_REPORT_TABLE)
        db.execSQL(CREATE_USER_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE conf;")
        db.execSQL("DROP TABLE element;")
        db.execSQL("DROP TABLE event;")
        db.execSQL("DROP TABLE report;")
        db.execSQL("DROP TABLE user;")
        onCreate(db)
    }

    companion object {
        const val CREATE_ELEMENT_TABLE = """
            CREATE TABLE element (
                id INTEGER NOT NULL PRIMARY KEY,
                overpass_data TEXT NOT NULL,
                tags TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                ext_lat REAL NOT NULL,
                ext_lon REAL NOT NULL
            );
            """

        const val CREATE_AREA_TABLE = """
            CREATE TABLE IF NOT EXISTS area (
                id INTEGER NOT NULL PRIMARY KEY,
                tags TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """

        const val CREATE_EVENT_TABLE = """
            CREATE TABLE event (
                id INTEGER NOT NULL PRIMARY KEY,
                user_id INTEGER NOT NULL,
                element_id INTEGER NOT NULL,
                type INTEGER NOT NULL,
                tags TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """

        const val CREATE_REPORT_TABLE = """
            CREATE TABLE report (
                id INTEGER PRIMARY KEY,
                area_id INTEGER NOT NULL,
                date TEXT NOT NULL,
                tags TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """

        const val CREATE_USER_TABLE = """
            CREATE TABLE user (
                id INTEGER NOT NULL PRIMARY KEY,
                osm_data TEXT NOT NULL,
                tags TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """

        const val CREATE_CONF_TABLE = """
            CREATE TABLE conf (
                last_sync_date TEXT NOT NULL,
                viewport_north_lat REAL NOT NULL,
                viewport_east_lon REAL NOT NULL,
                viewport_south_lat REAL NOT NULL,
                viewport_west_lon REAL NOT NULL,
                show_atms INTEGER NOT NULL,
                show_osm_attribution INTEGER NOT NULL,
                show_sync_summary INTEGER NOT NULL,
                show_all_new_elements INTEGER NOT NULL
            );
            """
    }
}