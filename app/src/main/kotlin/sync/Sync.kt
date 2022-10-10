package sync

import android.util.Log
import areas.AreasRepo
import conf.ConfRepo
import reports.ReportsRepo
import elements.ElementsRepo
import org.koin.core.annotation.Single
import users.UsersRepo
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Single
class Sync(
    private val areasRepo: AreasRepo,
    private val confRepo: ConfRepo,
    private val elementsRepo: ElementsRepo,
    private val reportsRepo: ReportsRepo,
    private val usersRepo: UsersRepo,
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

        runCatching {
            Log.d(TAG, "Syncing reports")
            reportsRepo.sync()
        }.onSuccess {
            Log.d(TAG, "Synced reports")
        }.onFailure {
            Log.d(TAG, "Failed to sync reports")
        }

        runCatching {
            Log.d(TAG, "Syncing areas")
            areasRepo.sync()
        }.onSuccess {
            Log.d(TAG, "Synced areas")
        }.onFailure {
            Log.d(TAG, "Failed to sync areas")
        }

        runCatching {
            Log.d(TAG, "Syncing users")
            usersRepo.sync()
        }.onSuccess {
            Log.d(TAG, "Synced users")
        }.onFailure {
            Log.d(TAG, "Failed to sync users")
        }

        confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
    }

    companion object {
        private const val TAG = "sync"
    }
}