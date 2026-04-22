package org.btcmap.db.table.user

const val TABLE = "user"

const val ID = "id"
const val NAME = "name"
const val ROLES = "roles"
const val SAVED_PLACES = "saved_places"
const val SAVED_AREAS = "saved_areas"

const val CREATE = """
    CREATE TABLE $TABLE (
        $ID INTEGER PRIMARY KEY NOT NULL,
        $NAME TEXT NOT NULL,
        $ROLES TEXT NOT NULL DEFAULT '[]',
        $SAVED_PLACES TEXT NOT NULL DEFAULT '[]',
        $SAVED_AREAS TEXT NOT NULL DEFAULT '[]'
    );
"""