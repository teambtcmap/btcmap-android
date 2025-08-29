package map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteConnection
import element.Element
import element.ElementsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLngBounds

class MapModel(
    val elementsRepo: ElementsRepo,
    private val conn: SQLiteConnection,
) : ViewModel() {

    private val _selectedElement: MutableStateFlow<Element?> = MutableStateFlow(null)
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
                        val clusters = elementsRepo.selectByBoundingBox(
                            zoom = zoom,
                            bounds = bounds,
                        )
                        _items.update { clusters.map { MapItem.ElementsCluster(it) } }
                    }

                    Filter.Events -> {
                        val meetups =
                            event.selectAll(conn).map { MapItem.Event(it) }
                        _items.update { meetups }
                    }

                    Filter.Exchanges -> {
                        val clusters = elementsRepo.selectExchangesByBoundingBox(
                            zoom = zoom,
                            bounds = bounds,
                        )
                        _items.update { clusters.map { MapItem.ElementsCluster(it) } }
                    }
                }
            }
        }
    }

    fun selectElement(elementId: Long) {
        val element = runBlocking { elementsRepo.selectById(elementId) }
        _selectedElement.update { element }
    }

    sealed class MapItem {
        data class ElementsCluster(val cluster: element.ElementsCluster) : MapItem()
        data class Event(val event: event.Event) : MapItem()
    }
}