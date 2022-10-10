package sync

import android.util.Log
import conf.ConfRepo
import reports.ReportsRepo
import elements.ElementsRepo
import org.koin.core.annotation.Single
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Single
class Sync(
    private val confRepo: ConfRepo,
    private val dailyReportsRepo: ReportsRepo,
    private val elementsRepo: ElementsRepo,
) {

    suspend fun sync() {
        elementsRepo.fetchBundledElements()

        val lastSyncDateTime = confRepo.conf.value.lastSyncDate
        val minSyncIntervalExpiryDate = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(15)
        Log.d(TAG, "Last sync date: $lastSyncDateTime")
        Log.d(TAG, "Min sync interval expiry date: $minSyncIntervalExpiryDate")

        if (lastSyncDateTime != null && lastSyncDateTime.isAfter(minSyncIntervalExpiryDate)) {
            Log.d(TAG, "Cache is up to date")
            return
        }

        Log.d(TAG, "Syncing elements")

        runCatching {
            elementsRepo.sync()
        }.onFailure {
            Log.e(TAG, "Failed to sync elements", it)
            return
        }.getOrThrow()

        Log.d(TAG, "Synced elements")
        Log.d(TAG, "Syncing daily reports")

        runCatching {
            dailyReportsRepo.sync()
        }.onSuccess {
            Log.d(TAG, "Synced daily reports")
        }.onFailure {
            Log.d(TAG, "Failed to sync daily reports")
        }

        confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
    }

    companion object {
        private const val TAG = "sync"
    }
}