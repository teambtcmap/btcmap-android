package org.btcmap.db.table.comment

const val TABLE = "comment"

const val ID = "id"
const val PLACE_ID = "place_id"
const val COMMENT = "comment"
const val CREATED_AT = "created_at"
const val UPDATED_AT = "updated_at"

const val CREATE = """
    CREATE TABLE $TABLE (
        $ID INTEGER PRIMARY KEY NOT NULL,
        $PLACE_ID INTEGER NOT NULL,
        $COMMENT TEXT NOT NULL,
        $CREATED_AT TEXT NOT NULL,
        $UPDATED_AT TEXT NOT NULL
    );
 """