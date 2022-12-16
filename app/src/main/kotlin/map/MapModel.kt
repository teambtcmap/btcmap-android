package map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import conf.ConfRepo
import elements.Element
import elements.ElementsCluster
import elements.ElementsRepo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import location.UserLocationRepository
import org.koin.android.annotation.KoinViewModel
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import sync.Sync

@KoinViewModel
class MapModel(
    val conf: ConfRepo,
    private val locationRepo: UserLocationRepository,
    private val sync: Sync,
    private val elementsRepo: ElementsRepo,
) : ViewModel() {

    val userLocation: StateFlow<GeoPoint?> = locationRepo.location

    data class MapViewport(
        val zoom: Double?,
        val boundingBox: BoundingBox,
    )

    private val _mapViewport: MutableStateFlow<MapViewport> = MutableStateFlow(
        MapViewport(
            zoom = null,
            boundingBox = BoundingBox(
                conf.conf.value.viewportNorthLat,
                conf.conf.value.viewportEastLon,
                conf.conf.value.viewportSouthLat,
                conf.conf.value.viewportWestLon,
            ),
        )
    )

    val mapViewport = _mapViewport.asStateFlow()

    private val _selectedElement: MutableStateFlow<Element?> = MutableStateFlow(null)
    val selectedElement = _selectedElement.asStateFlow()

    private val _visibleElements = MutableStateFlow<List<ElementsCluster>>(emptyList())
    val visibleElements = _visibleElements.asStateFlow()

    init {
        combine(
            mapViewport,
            conf.conf.map { it.lastSyncDate }
        ) { viewport, _ ->
            withContext(Dispatchers.Default) {
                _visibleElements.update {
                    elementsRepo.selectByBoundingBox(
                        zoom = viewport.zoom,
                        box = viewport.boundingBox,
                    )
                }
            }
        }.launchIn(viewModelScope)

        locationRepo.requestLocationUpdates()
    }

    fun onLocationPermissionGranted() {
        locationRepo.requestLocationUpdates()

        viewModelScope.launch {
            val location = withTimeout(5_000) { userLocation.filterNotNull().first() }
            setMapViewport(MapViewport(null, location.toBoundingBox(1_000.0)))
        }
    }

    fun setMapViewport(viewport: MapViewport) {
        _mapViewport.update { viewport }

        conf.update {
            it.copy(
                viewportNorthLat = viewport.boundingBox.latNorth,
                viewportEastLon = viewport.boundingBox.lonEast,
                viewportSouthLat = viewport.boundingBox.latSouth,
                viewportWestLon = viewport.boundingBox.lonWest,
            )
        }
    }

    fun selectElement(elementId: String, moveToLocation: Boolean) {
        val element = runBlocking { elementsRepo.selectById(elementId) }
        _selectedElement.update { element }

        if (element != null && moveToLocation) {
            _mapViewport.update {
                MapViewport(
                    null,
                    GeoPoint(element.lat, element.lon).toBoundingBox(100.0),
                )
            }
        }
    }

    fun syncElements() {
        viewModelScope.launch {
            runCatching { sync.sync() }
        }
    }

    private fun GeoPoint.toBoundingBox(distance: Double): BoundingBox {
        val point1 = destinationPoint(distance, 45.0)
        val point2 = destinationPoint(distance, -135.0)
        return BoundingBox.fromGeoPoints(listOf(point1, point2))
    }
}