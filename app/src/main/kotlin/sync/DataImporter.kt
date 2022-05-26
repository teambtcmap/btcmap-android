package sync

import android.util.Log
import db.Database
import db.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single

@Single
class DataImporter(
    private val db: Database,
) {

    companion object {
        private const val TAG = "DataImporter"
    }

    suspend fun import(data: JsonObject) {
        withContext(Dispatchers.Default) {
            val elements = data["elements"]!!.jsonArray
            Log.d(TAG, "Got ${elements.size} elements")

            db.transaction {
                db.placeQueries.deleteAll()

                for (e in elements) {
                    val element = e.jsonObject

                    val lat: Double
                    val lon: Double

                    val boundsMinLat: Double?
                    val boundsMinLon: Double?
                    val boundsMaxLat: Double?
                    val boundsMaxLon: Double?

                    if (element["type"]!!.jsonPrimitive.content == "node") {
                        lat = element["lat"]!!.jsonPrimitive.double
                        lon = element["lon"]!!.jsonPrimitive.double

                        boundsMinLat = null
                        boundsMinLon = null
                        boundsMaxLat = null
                        boundsMaxLon = null
                    } else {
                        val bounds = element["bounds"]!!.jsonObject

                        boundsMinLat = bounds["minlat"]!!.jsonPrimitive.double
                        boundsMinLon = bounds["minlon"]!!.jsonPrimitive.double
                        boundsMaxLat = bounds["maxlat"]!!.jsonPrimitive.double
                        boundsMaxLon = bounds["maxlon"]!!.jsonPrimitive.double

                        lat = (boundsMinLat + boundsMaxLat) / 2.0
                        lon = (boundsMinLon + boundsMaxLon) / 2.0
                    }

                    db.placeQueries.insert(
                        Place(
                            id = element["id"]!!.jsonPrimitive.content,
                            type = element["type"]!!.jsonPrimitive.content,
                            lat = lat,
                            lon = lon,
                            timestamp = element["timestamp"]!!.jsonPrimitive.content,
                            boundsMinLat = boundsMinLat,
                            boundsMinLon = boundsMinLon,
                            boundsMaxLat = boundsMaxLat,
                            boundsMaxLon = boundsMaxLon,
                            tags = element["tags"]!!.jsonObject,
                        )
                    )
                }
            }

            Log.d(TAG, "Finished importing the data")
        }
    }
}