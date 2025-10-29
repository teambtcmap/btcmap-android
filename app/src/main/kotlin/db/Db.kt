package db

import android.database.sqlite.SQLiteDatabase
import app.App

lateinit var db: SQLiteDatabase

fun init(app: App) {
    db = DbHelper(app).writableDatabase
}