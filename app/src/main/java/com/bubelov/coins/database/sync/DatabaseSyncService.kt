package com.bubelov.coins.database.sync

import android.app.IntentService
import android.content.Context
import android.content.Intent

import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class DatabaseSyncService : IntentService(DatabaseSyncService::class.java.simpleName) {
    @Inject internal lateinit var databaseSync: DatabaseSync

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onHandleIntent(intent: Intent?) {
        databaseSync.sync()
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, DatabaseSyncService::class.java))
        }
    }
}