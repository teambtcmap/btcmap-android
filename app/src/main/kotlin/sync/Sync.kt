package sync

import android.util.Log
import db.Database
import db.Place
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
            val url = "https://raw.githubusercontent.com/bubelov/btcmap-data/main/data.json"
            val request = Request.Builder().get().url(url).build()
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

                        val boundsMinLat: Double?
                        val boundsMinLon: Double?
                        val boundsMaxLat: Double?
                        val boundsMaxLon: Double?

                        if (place["type"].toString() == "node") {
                            lat = place.getDouble("lat")
                            lon = place.getDouble("lon")

                            boundsMinLat = null
                            boundsMinLon = null
                            boundsMaxLat = null
                            boundsMaxLon = null
                        } else {
                            val bounds = place.getJSONObject("bounds")
                            boundsMinLat = bounds.getDouble("minlat")
                            boundsMinLon = bounds.getDouble("minlon")
                            boundsMaxLat = bounds.getDouble("maxlat")
                            boundsMaxLon = bounds.getDouble("maxlon")

                            lat = (boundsMinLat + boundsMaxLat) / 2.0
                            lon = (boundsMinLon + boundsMaxLon) / 2.0
                        }

                        db.placeQueries.insert(
                            Place(
                                id = place.getString("id"),
                                type = place.getString("type"),
                                lat = lat,
                                lon = lon,
                                timestamp = place.getString("timestamp"),
                                boundsMinLat = boundsMinLat,
                                boundsMinLon = boundsMinLon,
                                boundsMaxLat = boundsMaxLat,
                                boundsMaxLon = boundsMaxLon,
                                tags = place.getJSONObject("tags"),
                            )
                        )
                    }
                }

                Log.d("Sync", "Finished sync")
            }
        }
    }
}