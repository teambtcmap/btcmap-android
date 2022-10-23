package map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import conf.ConfRepo
import db.Database
import db.Element
import elements.ElementsRepo
import icons.toIconResId
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
    private val db: Database,
    private val elementsRepo: ElementsRepo,
) : ViewModel() {

    private val mapMarkersRepo: MutableStateFlow<MapMarkersRepo?> = MutableStateFlow(null)

    val userLocation: StateFlow<GeoPoint?> = locationRepo.location

    private val _mapBoundingBox: MutableStateFlow<BoundingBox> = MutableStateFlow(
        BoundingBox(
            conf.conf.value.viewport_north_lat,
            conf.conf.value.viewport_east_lon,
            conf.conf.value.viewport_south_lat,
            conf.conf.value.viewport_west_lon,
        )
    )

    val mapBoundingBox = _mapBoundingBox.asStateFlow()

    private val _selectedElement: MutableStateFlow<Element?> = MutableStateFlow(null)
    val selectedElement = _selectedElement.asStateFlow()

    private val _visibleElements = MutableStateFlow<List<ElementWithMarker>>(emptyList())
    val visibleElements = _visibleElements.asStateFlow()

    init {
        combine(
            _mapBoundingBox,
            db.elementQueries.selectCount().asFlow().mapToOne(Dispatchers.IO),
            mapMarkersRepo,
        ) { viewport, _, mapMarkersRepo ->
            withContext(Dispatchers.Default) {
                if (mapMarkersRepo == null) {
                    return@withContext
                }

                _visibleElements.update {
                    elementsRepo.selectByBoundingBox(viewport)
                        .map {
                            ElementWithMarker(
                                it,
                                mapMarkersRepo.getMarker(it.icon_id.toIconResId()),
                            )
                        }
                }
            }
        }.launchIn(viewModelScope)

        locationRepo.requestLocationUpdates()
    }

    fun setArgs(context: Context) {
        mapMarkersRepo.update { MapMarkersRepo(context, conf) }
    }

    fun onLocationPermissionGranted() {
        locationRepo.requestLocationUpdates()

        viewModelScope.launch {
            val location = withTimeout(5_000) { userLocation.filterNotNull().first() }
            setMapViewport(location.toBoundingBox(1_000.0))
        }
    }

    fun setMapViewport(viewport: BoundingBox) {
        _mapBoundingBox.update { viewport }

        conf.update {
            it.copy(
                viewport_north_lat = viewport.latNorth,
                viewport_east_lon = viewport.lonEast,
                viewport_south_lat = viewport.latSouth,
                viewport_west_lon = viewport.lonWest,
            )
        }
    }

    fun selectElement(elementId: String, moveToLocation: Boolean) {
        val element = runBlocking {
            db.elementQueries.selectById(elementId).asFlow().mapToOneOrNull(Dispatchers.IO).first()
        }
        _selectedElement.update { element }

        if (element != null && moveToLocation) {
            _mapBoundingBox.update {
                GeoPoint(element.lat, element.lon).toBoundingBox(100.0)
            }
        }
    }

    fun invalidateMarkersCache() {
        mapMarkersRepo.value?.invalidateCache()
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