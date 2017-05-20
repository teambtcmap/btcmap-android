package com.bubelov.coins.database.sync

import android.app.IntentService
import android.content.Context
import android.content.Intent

import com.bubelov.coins.dagger.Injector

/**
 * @author Igor Bubelov
 */

class DatabaseSyncService : IntentService(DatabaseSyncService::class.java.simpleName) {
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        val databaseSync = Injector.INSTANCE.mainComponent().databaseSync()

        if (!databaseSync.isSyncing) {
            databaseSync.sync()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, DatabaseSyncService::class.java))
        }
    }
}