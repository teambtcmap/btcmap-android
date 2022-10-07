package sync

import android.util.Log
import db.Database
import db.Element
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.koin.core.annotation.Single

@Single
class DataImporter(
    private val db: Database,
) {

    companion object {
        private const val TAG = "DataImporter"
    }

    suspend fun import(elements: JsonArray) {
        withContext(Dispatchers.Default) {
            Log.d(TAG, "Got ${elements.size} elements")

            db.transaction {
                for (element in elements) {

                    val lat: Double
                    val lon: Double

                    val osmJson = element.jsonObject["data"]!!.jsonObject

                    if (osmJson["type"]!!.jsonPrimitive.content == "node") {
                        lat = osmJson["lat"]!!.jsonPrimitive.double
                        lon = osmJson["lon"]!!.jsonPrimitive.double
                    } else {
                        val bounds = osmJson["bounds"]!!.jsonObject

                        val boundsMinLat = bounds["minlat"]!!.jsonPrimitive.double
                        val boundsMinLon = bounds["minlon"]!!.jsonPrimitive.double
                        val boundsMaxLat = bounds["maxlat"]!!.jsonPrimitive.double
                        val boundsMaxLon = bounds["maxlon"]!!.jsonPrimitive.double

                        lat = (boundsMinLat + boundsMaxLat) / 2.0
                        lon = (boundsMinLon + boundsMaxLon) / 2.0
                    }

                    db.elementQueries.insertOrReplace(
                        Element(
                            id = element.jsonObject["id"]!!.jsonPrimitive.content,
                            lat = lat,
                            lon = lon,
                            osm_json = osmJson.toString(),
                            created_at = element.jsonObject["created_at"]!!.jsonPrimitive.content,
                            updated_at = element.jsonObject["updated_at"]!!.jsonPrimitive.content,
                            deleted_at = element.jsonObject["deleted_at"]!!.jsonPrimitive.contentOrNull
                                ?: "",
                        )
                    )
                }
            }

            Log.d(TAG, "Finished importing the data")
        }
    }
}