package sync

import android.content.Context
import android.util.Log
import db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koin.core.annotation.Single

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
            Log.d(TAG, "Starting sync")

            if (db.placeQueries.selectCount().executeAsOne() == 0L) {
                Log.d(TAG, "Importing bundled data")

                runCatching {
                    val bundledData = context.assets.open("data.json")
                    dataImporter.import(JSONObject(bundledData.bufferedReader().readText()))
                }.onFailure {
                    Log.e(TAG, "Failed to import bundled data", it)
                }
            }

            Log.d(TAG, "Syncing with $BTCMAP_DATA_URL")

            if (!sync(BTCMAP_DATA_URL)) {
                Log.w(TAG, "Failed to sync with $BTCMAP_DATA_URL")
                Log.d(TAG, "Syncing with $GITHUB_DATA_URL")

                if (!sync(GITHUB_DATA_URL)) {
                    Log.w(TAG, "Failed to sync with $GITHUB_DATA_URL")
                }
            }

            Log.d(TAG, "Finished sync")
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
}