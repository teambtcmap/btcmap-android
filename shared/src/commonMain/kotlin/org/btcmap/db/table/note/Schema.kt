package org.btcmap.db.table.note

const val TABLE = "note"

const val ID = "id"
const val LAT = "lat"
const val LON = "lon"
const val TEXT = "text"
const val ICON = "icon"
const val IS_PUBLIC = "is_public"
const val CREATED_AT = "created_at"
const val UPDATED_AT = "updated_at"
const val DELETED_AT = "deleted_at"

const val CREATE = """
    CREATE TABLE $TABLE (
        $ID INTEGER PRIMARY KEY NOT NULL,
        $LAT REAL NOT NULL,
        $LON REAL NOT NULL,
        $TEXT TEXT NOT NULL,
        $ICON TEXT NOT NULL DEFAULT 'notes',
        $IS_PUBLIC INTEGER NOT NULL,
        $CREATED_AT TEXT NOT NULL,
        $UPDATED_AT TEXT NOT NULL,
        $DELETED_AT TEXT
    );
 """

// Serves selectMaxUpdatedAt, the delta-sync cursor: the expression index lets
// SQLite read the newest row directly instead of scanning and sorting the whole
// table, mirroring the other tables.
const val INDEX_UPDATED_AT = "note_updated_at"
const val CREATE_INDEX_UPDATED_AT =
    "CREATE INDEX IF NOT EXISTS $INDEX_UPDATED_AT ON $TABLE(julianday($UPDATED_AT));"
