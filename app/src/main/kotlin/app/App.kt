package app

import android.app.Application
import db.DbHelper
import db.db
import settings.prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        db = DbHelper(this).writableDatabase
    }
}