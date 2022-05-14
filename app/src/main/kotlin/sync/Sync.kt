package sync

import android.util.Log
import com.google.gson.Gson
import db.Database
import db.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.time.ZonedDateTime

@Single
class Sync(
    private val db: Database,
) {

    suspend fun sync() {
        withContext(Dispatchers.Default) {
            val maxUpdatedAt = db.placeQueries.selectMaxUpdatedAt().executeAsOneOrNull()?.MAX ?: "2000-01-01T00:00:00Z"
            val afterMaxUpdatedAt = ZonedDateTime.parse(maxUpdatedAt).toString()

            val httpClient = OkHttpClient()

            val request = Request.Builder().get()
                .url("https://api.btcmap.org/places?created_or_updated_since=$afterMaxUpdatedAt")
                .build()

            val call = httpClient.newCall(request)
            val response = call.execute()

            if (response.isSuccessful) {
                val json = response.body!!.string()
                val places: List<Place> = Gson().fromJson(json, Array<Place>::class.java).toList()
                Log.d("Sync", "Got ${places.size} places")

                db.transaction {
                    places.forEach { db.placeQueries.insertOrReplace(it) }
                }
            }
        }
    }
}