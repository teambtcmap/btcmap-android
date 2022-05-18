package sync

import android.util.Log
import db.Database
import db.Place
import db.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
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
                val places = JSONArray(json)
                Log.d("Sync", "Got ${places.length()} places")

                db.transaction {
                    for (i in 0 until places.length()) {
                        val place = places.getJSONObject(i)

                        db.placeQueries.insertOrReplace(
                            Place(
                                id = place.getString("id"),
                                lat = place.getDouble("lat"),
                                lon = place.getDouble("lon"),
                                tags = Tags(place.getJSONObject("tags")),
                                created_at = place.getString("created_at"),
                                updated_at = place.getString("updated_at"),
                                deleted_at = place.getString("deleted_at"),
                            )
                        )
                    }
                }

                Log.d("Sync", "Finished sync")
            }
        }
    }
}