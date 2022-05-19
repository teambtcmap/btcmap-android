package sync

import android.util.Log
import db.Database
import db.Place
import db.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koin.core.annotation.Single

@Single
class Sync(
    private val db: Database,
) {

    suspend fun sync() {
        withContext(Dispatchers.Default) {
            val httpClient = OkHttpClient()
            val request = Request.Builder().get().url("https://api.btcmap.org/data").build()
            val call = httpClient.newCall(request)
            val response = call.execute()

            if (response.isSuccessful) {
                val json = response.body!!.string()
                val elements = JSONObject(json).getJSONArray("elements")
                Log.d("Sync", "Got ${elements.length()} elements")

                db.transaction {
                    db.placeQueries.deleteAll()

                    for (i in 0 until elements.length()) {
                        val place = elements.getJSONObject(i)

                        val lat: Double
                        val lon: Double

                        if (place["type"].toString() == "node") {
                            lat = place.getDouble("lat")
                            lon = place.getDouble("lon")
                        } else {
                            val center = place.getJSONObject("center")
                            lat = center.getDouble("lat")
                            lon = center.getDouble("lon")
                        }

                        db.placeQueries.insert(
                            Place(
                                id = place.getString("id"),
                                type = place.getString("type"),
                                lat = lat,
                                lon = lon,
                                tags = Tags(place.getJSONObject("tags")),
                            )
                        )
                    }
                }

                Log.d("Sync", "Finished sync")
            }
        }
    }
}