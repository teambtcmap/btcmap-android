package sync

import android.content.Context
import android.util.Log
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import conf.ConfRepo
import dailyreports.DailyReportsRepo
import db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Single
class Sync(
    private val dataImporter: DataImporter,
    private val confRepo: ConfRepo,
    private val db: Database,
    private val context: Context,
    private val dailyReportsRepo: DailyReportsRepo,
) {

    suspend fun sync() {
        if (db.elementQueries.selectCount().asFlow().mapToOne().first() == 0L) {
            Log.d(TAG, "Importing bundled data")

            withContext(Dispatchers.Default) {
                runCatching {
                    val bundledData = context.assets.open("elements.json")
                    val bundledDataJson: JsonArray =
                        Json.decodeFromString(bundledData.bufferedReader().readText())
                    dataImporter.import(bundledDataJson)
                }.onFailure {
                    Log.e(TAG, "Failed to import bundled data", it)
                }
            }
        }

        val lastSyncDateTime = confRepo.conf.value.lastSyncDate
        val minSyncIntervalExpiryDate = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(15)
        Log.d(TAG, "Last sync date: $lastSyncDateTime")
        Log.d(TAG, "Min sync interval expiry date: $minSyncIntervalExpiryDate")

        if (lastSyncDateTime != null && lastSyncDateTime.isAfter(minSyncIntervalExpiryDate)) {
            Log.d(TAG, "Cache is up to date")
            return
        }

        val maxUpdatedAt = withContext(Dispatchers.Default) {
            db.elementQueries.selectMaxUpdatedAt().executeAsOneOrNull()?.MAX
        }

        val url = if (maxUpdatedAt == null) {
            "https://api.btcmap.org/elements"
        } else {
            "https://api.btcmap.org/elements?updated_since=$maxUpdatedAt"
        }.toHttpUrl()

        Log.d(TAG, "Syncing with $url")

        if (sync(url)) {
            confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
            Log.d(TAG, "Finished sync")
        } else {
            Log.w(TAG, "Failed to sync with $url")
        }

        runCatching {
            dailyReportsRepo.sync()
        }
    }

    private suspend fun sync(url: HttpUrl): Boolean {
        return withContext(Dispatchers.Default) {
            val httpClient = OkHttpClient()
            val request = Request.Builder().get().url(url).build()
            val call = httpClient.newCall(request)
            val response = runCatching { call.execute() }.getOrNull() ?: return@withContext false

            if (response.isSuccessful) {
                runCatching {
                    val json: JsonArray = Json.decodeFromString(response.body!!.string())
                    Log.d(TAG, "Got ${json.size} elements")
                    dataImporter.import(json)
                }.onFailure {
                    Log.e(TAG, "Failed to import new data", it)
                }.isSuccess
            } else {
                false
            }
        }
    }

    companion object {
        private const val TAG = "sync"
    }
}