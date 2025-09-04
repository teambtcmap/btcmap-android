package element

import api.Api
import db.db
import db.table.place.Cluster
import db.table.place.Place
import db.table.place.PlaceQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import kotlin.math.pow

class ElementsRepo(
    private val api: Api,
) {

    suspend fun selectById(id: Long): Place {
        return withContext(Dispatchers.IO) {
            PlaceQueries.selectById(id, db)
        }
    }

    suspend fun selectBySearchString(searchString: String): List<Place> {
        return withContext(Dispatchers.IO) {
            PlaceQueries.selectBySearchString(searchString, db)
        }
    }

    suspend fun selectByBoundingBox(
        zoom: Double?,
        bounds: LatLngBounds,
        includeMerchants: Boolean,
        includeExchanges: Boolean,
    ): List<Cluster> {
        if (zoom == null) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            if (zoom > 18) {
                withContext(Dispatchers.IO) {
                    PlaceQueries.selectWithoutClustering(
                        minLat = bounds.latitudeSouth,
                        maxLat = bounds.latitudeNorth,
                        minLon = bounds.longitudeWest,
                        maxLon = bounds.longitudeEast,
                        includeMerchants = includeMerchants,
                        includeExchanges = includeExchanges,
                        db,
                    )
                }
            } else {
                val step = 50.0 / 2.0.pow(zoom)
                withContext(Dispatchers.IO) {
                    val clusters = PlaceQueries.selectClusters(
                        step / 2,
                        step,
                        includeMerchants = includeMerchants,
                        includeExchanges = includeExchanges,
                        db,
                    )
                    clusters.filter { bounds.contains(LatLng(it.lat, it.lon)) }
                }
            }
        }
    }
}