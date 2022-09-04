package sync

import android.content.Context
import android.util.Log
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import conf.ConfRepo
import db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
) {

    companion object {
        private const val TAG = "sync"

        private val BTCMAP_DATA_URL = "https://btcmap.org/data.json".toHttpUrl()
        private val GITHUB_DATA_URL =
            "https://raw.githubusercontent.com/teambtcmap/btcmap-data/main/data.json".toHttpUrl()
    }

    suspend fun sync() {
        if (db.elementQueries.selectCount().asFlow().mapToOne().first() == 0L) {
            Log.d(TAG, "Importing bundled data")

            withContext(Dispatchers.Default) {
                runCatching {
                    val bundledData = context.assets.open("data.json")
                    val bundledDataJson: JsonObject = Json.decodeFromString(bundledData.bufferedReader().readText())
                    dataImporter.import(bundledDataJson)
                }.onFailure {
                    Log.e(TAG, "Failed to import bundled data", it)
                }
            }
        }

        val lastSyncDateTime = confRepo.conf.value.lastSyncDate
        Log.d(TAG, "Last sync date: $lastSyncDateTime")

        Log.d(TAG, "Syncing with $BTCMAP_DATA_URL")

        if (sync(BTCMAP_DATA_URL)) {
            confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
            Log.d(TAG, "Finished sync")
        } else {
            Log.w(TAG, "Failed to sync with $BTCMAP_DATA_URL")
            Log.d(TAG, "Syncing with $GITHUB_DATA_URL")

            if (sync(GITHUB_DATA_URL)) {
                confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
                Log.d(TAG, "Finished sync")
            } else {
                Log.w(TAG, "Failed to sync with $GITHUB_DATA_URL")
            }
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
                    val json: JsonObject = Json.decodeFromString(response.body!!.string())
                    dataImporter.import(json)
                }.onFailure {
                    Log.e(TAG, "Failed to import new data", it)
                }.isSuccess
            } else {
                false
            }
        }
    }
}