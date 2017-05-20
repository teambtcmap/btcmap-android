package com.bubelov.coins.database.sync

import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.GcmTaskService
import com.google.android.gms.gcm.TaskParams

/**
 * @author Igor Bubelov
 */

class DatabaseGcmSyncService : GcmTaskService() {
    override fun onInitializeTasks() {
        onRunTask(null)
    }

    override fun onRunTask(taskParams: TaskParams?): Int {
        DatabaseSyncService.start(this)
        return GcmNetworkManager.RESULT_SUCCESS
    }

    companion object {
        const val TAG = "DATABASE_GCM_SYNC_SERVICE"
    }
}