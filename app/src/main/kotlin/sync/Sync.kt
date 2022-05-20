package sync

import android.content.Context
import android.util.Log
import db.Conf
import db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koin.core.annotation.Single
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Single
class Sync(
    private val dataImporter: DataImporter,
    private val db: Database,
    private val context: Context,
) {

    companion object {
        private const val TAG = "Sync"

        private val BTCMAP_DATA_URL = "https://api.btcmap.org/data".toHttpUrl()
        private val GITHUB_DATA_URL = "https://raw.githubusercontent.com/bubelov/btcmap-data/main/data.json".toHttpUrl()
    }

    suspend fun sync() {
        withContext(Dispatchers.Default) {
            if (db.placeQueries.selectCount().executeAsOne() == 0L) {
                Log.d(TAG, "Importing bundled data")

                runCatching {
                    val bundledData = context.assets.open("data.json")
                    dataImporter.import(JSONObject(bundledData.bufferedReader().readText()))
                }.onFailure {
                    Log.e(TAG, "Failed to import bundled data", it)
                }
            }

            val lastSyncDateTime = db.confQueries.select().executeAsOneOrNull()?.lastSyncDateTime
            val hourAgo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(1)
            Log.d(TAG, "Last sync date: $lastSyncDateTime")
            Log.d(TAG, "Hour ago: $hourAgo")

            if (lastSyncDateTime != null && ZonedDateTime.parse(lastSyncDateTime).isAfter(hourAgo)) {
                Log.d(TAG, "Data is up to date")
                return@withContext
            }

            Log.d(TAG, "Syncing with $BTCMAP_DATA_URL")

            if (sync(BTCMAP_DATA_URL)) {
                setLastSyncDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                Log.d(TAG, "Finished sync")
            } else {
                Log.w(TAG, "Failed to sync with $BTCMAP_DATA_URL")
                Log.d(TAG, "Syncing with $GITHUB_DATA_URL")

                if (sync(GITHUB_DATA_URL)) {
                    setLastSyncDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                    Log.d(TAG, "Finished sync")
                } else {
                    Log.w(TAG, "Failed to sync with $GITHUB_DATA_URL")
                }
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
                runCatching { dataImporter.import(JSONObject(response.body!!.string())) }.isSuccess
            } else {
                false
            }
        }
    }

    private suspend fun setLastSyncDateTime(lastSyncDateTime: ZonedDateTime) {
        withContext(Dispatchers.Default) {
            val conf = db.confQueries.select().executeAsOneOrNull() ?: Conf("")

            db.transaction {
                db.confQueries.delete()
                db.confQueries.insert(conf.copy(lastSyncDateTime = lastSyncDateTime.toString()))
            }
        }
    }
}