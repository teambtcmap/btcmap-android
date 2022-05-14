package map

import android.util.Log
import com.google.gson.Gson
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import db.Database
import db.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.time.ZonedDateTime

@Single
class PlacesRepository(
    private val db: Database,
) {

    fun selectAll(): Flow<List<Place>> {
        return db.placeQueries.selectAll().asFlow().mapToList()
    }

    fun selectById(id: Long): Flow<Place?> {
        return db.placeQueries.selectById(id).asFlow().mapToOneOrNull()
    }

    fun selectBySearchString(searchString: String): Flow<List<Place>> {
        return db.placeQueries.selectBySearchString(searchString).asFlow().mapToList()
    }

    fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): Flow<List<Place>> {
        return db.placeQueries.selectByBoundingBox(
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon
        ).asFlow().map { it.executeAsList() }
    }

    fun selectCount(): Flow<Long> {
        return db.placeQueries.selectCount().asFlow().mapToOne()
    }

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
                Log.d(PlacesRepository::class.java.simpleName, "Got ${places.size} places")

                db.transaction {
                    places.forEach { db.placeQueries.insertOrReplace(it) }
                }
            }
        }
    }
}