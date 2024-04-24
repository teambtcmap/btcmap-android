package sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.isDebuggable
import java.util.concurrent.TimeUnit

class BackgroundSyncScheduler(private val context: Context) {
    fun schedule() {
        Log.d("sync", "Scheduling background sync")
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val periodicSyncRequest = if (context.isDebuggable()) {
            Log.d("sync", "App is debuggable, scheduling hourly sync")
            PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
        } else {
            PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.DAYS)
        }.build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicSyncRequest,
        )
        Log.d("sync", "Scheduled background sync")
    }

    companion object {
        const val WORK_NAME = "sync"
    }
}