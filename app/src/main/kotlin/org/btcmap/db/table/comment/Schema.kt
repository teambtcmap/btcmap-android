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

// Serves selectByPlaceId: the leading column filters the place, and the
// expression matches the ORDER BY so the rows come back without a sort.
const val CREATE_INDEX_PLACE_ID_CREATED_AT =
    "CREATE INDEX comment_place_id_created_at ON $TABLE($PLACE_ID, julianday($CREATED_AT) DESC);"

// Serves selectMaxUpdatedAt: the expression index lets SQLite read the newest
// row directly instead of scanning and sorting the whole table.
const val CREATE_INDEX_UPDATED_AT =
    "CREATE INDEX comment_updated_at ON $TABLE(julianday($UPDATED_AT));"
