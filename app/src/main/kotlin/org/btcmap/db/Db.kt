package org.btcmap.db

import android.database.sqlite.SQLiteDatabase
import org.btcmap.app.App

lateinit var db: SQLiteDatabase

fun init(app: App) {
    db = DbHelper(app).writableDatabase
}