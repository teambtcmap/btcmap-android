package sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.App
import conf.ConfRepo
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get

class SyncWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork() = runBlocking { doWorkAsync() }

    private suspend fun doWorkAsync(): Result {
        val app = applicationContext as App
        val conf = app.get<ConfRepo>().conf.value
        val sync = app.get<Sync>()

        if (conf.lastSyncDate == null) {
            return Result.retry()
        }

        sync.sync(doNothingIfAlreadySyncing = true)

        return Result.success()
    }
}