package org.btcmap.db.table.note

const val TABLE = "note"

const val ID = "id"
const val LAT = "lat"
const val LON = "lon"
const val TEXT = "text"
const val IS_PUBLIC = "is_public"
const val CREATED_AT = "created_at"
const val UPDATED_AT = "updated_at"

const val CREATE = """
    CREATE TABLE $TABLE (
        $ID INTEGER PRIMARY KEY NOT NULL,
        $LAT REAL NOT NULL,
        $LON REAL NOT NULL,
        $TEXT TEXT NOT NULL,
        $IS_PUBLIC INTEGER NOT NULL,
        $CREATED_AT TEXT NOT NULL,
        $UPDATED_AT TEXT NOT NULL
    );
 """
