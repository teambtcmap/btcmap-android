package app

import android.app.Application
import db.DbHelper
import db.db

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        db = DbHelper(this).writableDatabase
    }
}