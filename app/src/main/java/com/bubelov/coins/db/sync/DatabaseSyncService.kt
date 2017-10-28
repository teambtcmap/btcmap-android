package com.bubelov.coins.db.sync

import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.GcmTaskService
import com.google.android.gms.gcm.TaskParams
import dagger.android.AndroidInjection
import kotlinx.coroutines.experimental.runBlocking
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class DatabaseSyncService : GcmTaskService() {
    @Inject lateinit var databaseSync: DatabaseSync

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onInitializeTasks() {
        onRunTask(null)
    }

    override fun onRunTask(taskParams: TaskParams?): Int {
        runBlocking { databaseSync.start() }
        return GcmNetworkManager.RESULT_SUCCESS
    }

    companion object {
        const val TAG = "DATABASE_GCM_SYNC_SERVICE"
    }
}