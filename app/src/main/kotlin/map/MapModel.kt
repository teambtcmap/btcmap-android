package map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteConnection
import conf.ConfRepo
import element.Element
import element.ElementsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import sync.Sync

class MapModel(
    val conf: ConfRepo,
    private val sync: Sync,
    val elementsRepo: ElementsRepo,
    private val conn: SQLiteConnection,
) : ViewModel() {

    private val _selectedElement: MutableStateFlow<Element?> = MutableStateFlow(null)
    val selectedElement = _selectedElement.asStateFlow()

    private val _items = MutableStateFlow<List<MapItem>>(emptyList())
    val visibleElements = _items.asStateFlow()

    suspend fun loadData(bounds: LatLngBounds, zoom: Double) {
        withContext(Dispatchers.IO) {
            val clusters = elementsRepo.selectByBoundingBox(
                zoom = zoom,
                bounds = bounds,
            )
            val meetups = event.selectAll(conn).map { MapItem.Event(it) }
            _items.update { clusters.map { MapItem.ElementsCluster(it) } + meetups }
        }
    }

    fun selectElement(elementId: Long) {
        val element = runBlocking { elementsRepo.selectById(elementId) }
        _selectedElement.update { element }
    }

    fun syncElements() {
        viewModelScope.launch {
            sync.sync(doNothingIfAlreadySyncing = true)
        }
    }

    private fun LatLng.toBoundingBox(): LatLngBounds {
        val point1 = LatLng(latitude - 0.001, longitude - 0.001)
        val point2 = LatLng(latitude + 0.001, longitude + 0.001)
        return LatLngBounds.fromLatLngs(listOf(point1, point2))
    }

    sealed class MapItem {
        data class ElementsCluster(val cluster: element.ElementsCluster) : MapItem()
        data class Event(val event: event.Event) : MapItem()
    }
}