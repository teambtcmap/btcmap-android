package org.btcmap.db.table.preference

const val TABLE = "preference"

const val KEY = "pref_key"
const val VALUE = "pref_value"

const val CREATE = """
    CREATE TABLE $TABLE (
        $KEY TEXT PRIMARY KEY NOT NULL,
        $VALUE TEXT NOT NULL
    );
"""
