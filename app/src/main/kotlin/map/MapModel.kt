package map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import db.db
import db.table.event.EventQueries
import db.table.place.Cluster
import db.table.place.Place
import db.table.place.PlaceQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import kotlin.math.pow

class MapModel() : ViewModel() {

    private val _selectedElement: MutableStateFlow<Place?> = MutableStateFlow(null)
    val selectedElement = _selectedElement.asStateFlow()

    private val _items = MutableStateFlow<List<MapItem>>(emptyList())
    val items = _items.asStateFlow()

    enum class Filter {
        Merchants,
        Events,
        Exchanges,
    }

    private var prevLoadItems: Job? = null

    fun loadItems(bounds: LatLngBounds, zoom: Double, filter: Filter) {
        prevLoadItems?.cancel()

        prevLoadItems = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when (filter) {
                    Filter.Merchants -> {
                        val clusters = selectByBoundingBox(
                            zoom = zoom,
                            bounds = bounds,
                            includeMerchants = true,
                            includeExchanges = false,
                        )
                        _items.update { clusters.map { MapItem.ElementsCluster(it) } }
                    }

                    Filter.Events -> {
                        val meetups =
                            EventQueries.selectAll(db).map { MapItem.Event(it) }
                        _items.update { meetups }
                    }

                    Filter.Exchanges -> {
                        val clusters = selectByBoundingBox(
                            zoom = zoom,
                            bounds = bounds,
                            includeMerchants = false,
                            includeExchanges = true,
                        )
                        _items.update { clusters.map { MapItem.ElementsCluster(it) } }
                    }
                }
            }
        }
    }

    fun selectElement(elementId: Long) {
        if (elementId == 0L) {
            _selectedElement.update { null }
        } else {
            val element = PlaceQueries.selectById(elementId, db)
            _selectedElement.update { element }
        }
    }

    sealed class MapItem {
        data class ElementsCluster(val cluster: Cluster) : MapItem()
        data class Event(val event: db.table.event.Event) : MapItem()
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