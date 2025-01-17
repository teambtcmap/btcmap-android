package map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import area.AreasRepo
import conf.ConfRepo
import conf.mapViewport
import element.Element
import element.ElementsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import location.UserLocationRepository
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import sync.Sync

class MapModel(
    val conf: ConfRepo,
    private val locationRepo: UserLocationRepository,
    private val sync: Sync,
    private val elementsRepo: ElementsRepo,
    private val areasRepo: AreasRepo,
) : ViewModel() {

    val userLocation: StateFlow<LatLng?> = locationRepo.location

    data class MapViewport(
        val zoom: Double?,
        val boundingBox: LatLngBounds,
    )

    private val _mapViewport: MutableStateFlow<MapViewport> = MutableStateFlow(
        MapViewport(zoom = null, boundingBox = conf.conf.value.mapViewport())
    )

    val mapViewport = _mapViewport.asStateFlow()

    private val _excludedCategories = MutableStateFlow<List<String>>(emptyList())

    private val _selectedElement: MutableStateFlow<Element?> = MutableStateFlow(null)
    val selectedElement = _selectedElement.asStateFlow()

    private val _items = MutableStateFlow<List<MapItem>>(emptyList())
    val visibleElements = _items.asStateFlow()

    init {
        combine(
            mapViewport,
            db.elementsUpdatedAt,
            _excludedCategories
        ) { viewport, _, excludedCategories ->
            withContext(Dispatchers.IO) {
                val clusters = elementsRepo.selectByBoundingBox(
                    zoom = viewport.zoom,
                    bounds = viewport.boundingBox,
                    excludedCategories = excludedCategories.map { it },
                )
                val meetups = areasRepo.selectMeetups().map { MapItem.Meetup(it) }
                _items.update { clusters.map { MapItem.ElementsCluster(it) } + meetups }
            }
        }.launchIn(viewModelScope)

        locationRepo.requestLocationUpdates()
    }

    fun setExcludedCategories(excludedCategories: List<String>) {
        _excludedCategories.update { excludedCategories }
    }

    fun onLocationPermissionGranted() {
        locationRepo.requestLocationUpdates()

        viewModelScope.launch {
            val location = withTimeout(5_000) { userLocation.filterNotNull().first() }
            setMapViewport(MapViewport(null, location.toBoundingBox()))
        }
    }

    fun setMapViewport(viewport: MapViewport) {
        _mapViewport.update { viewport }

        conf.update {
            it.copy(
                viewportNorthLat = viewport.boundingBox.getLatNorth(),
                viewportEastLon = viewport.boundingBox.getLonEast(),
                viewportSouthLat = viewport.boundingBox.getLatSouth(),
                viewportWestLon = viewport.boundingBox.getLonWest(),
            )
        }
    }

    fun selectElement(elementId: Long, moveToLocation: Boolean) {
        val element = runBlocking { elementsRepo.selectById(elementId) }
        _selectedElement.update { element }

        if (element != null && moveToLocation) {
            _mapViewport.update {
                MapViewport(
                    null,
                    LatLng(element.lat, element.lon).toBoundingBox(),
                )
            }
        }
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
        data class Meetup(val meetup: area.Meetup) : MapItem()
    }
}