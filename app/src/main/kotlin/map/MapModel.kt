package map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import conf.ConfRepo
import db.Database
import db.Element
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import location.UserLocationRepository
import org.koin.android.annotation.KoinViewModel
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import sync.Sync
import kotlin.math.max
import kotlin.math.min

@KoinViewModel
class MapModel(
    val conf: ConfRepo,
    private val locationRepo: UserLocationRepository,
    private val sync: Sync,
    private val db: Database,
) : ViewModel() {

    private val mapMarkersRepo: MutableStateFlow<MapMarkersRepository?> = MutableStateFlow(null)

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

    private val _syncMessage: MutableStateFlow<String> = MutableStateFlow("")
    val syncMessage = _syncMessage.asStateFlow()

    init {
        combine(
            _mapBoundingBox,
            db.elementQueries.selectCount().asFlow().mapToOne(),
            mapMarkersRepo
        ) { viewport, _, mapMarkersRepo ->
            withContext(Dispatchers.Default) {
                if (mapMarkersRepo == null) {
                    return@withContext
                }

                _visibleElements.update {
                    db.elementQueries.selectByBoundingBox(
                        minLat = min(viewport.latNorth, viewport.latSouth),
                        maxLat = max(viewport.latNorth, viewport.latSouth),
                        minLon = min(viewport.lonEast, viewport.lonWest),
                        maxLon = max(viewport.lonEast, viewport.lonWest),
                    )
                        .asFlow()
                        .mapToList()
                        .first()
                        .map { ElementWithMarker(it, mapMarkersRepo.getMarker(it)) }
                }
            }
        }.launchIn(viewModelScope)

        locationRepo.requestLocationUpdates()
    }

    fun setArgs(context: Context) {
        mapMarkersRepo.update { MapMarkersRepository(context, conf) }
    }

    fun onLocationPermissionGranted() {
        locationRepo.requestLocationUpdates()
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

    fun selectElement(element: Element?, moveToLocation: Boolean) {
        _selectedElement.update { element }

        if (element != null && moveToLocation) {
            _mapBoundingBox.update { GeoPoint(element.lat, element.lon).toBoundingBox(100.0) }
        }
    }

    fun invalidateMarkersCache() {
        mapMarkersRepo.value?.invalidateCache()
    }

    fun syncElements() {
        viewModelScope.launch {
            runCatching {
                val job = launch {
                    delay(1000)
                    _syncMessage.update { "Syncing data" }
                }

                sync.sync()

                if (job.isCompleted) {
                    delay(1000)
                } else {
                    job.cancel()
                }

                _syncMessage.update { "" }
            }.onFailure {
                _syncMessage.update { "Failed to sync data" }
                delay(5000)
                _syncMessage.update { "" }
            }
        }
    }

    private fun GeoPoint.toBoundingBox(distance: Double): BoundingBox {
        val point1 = destinationPoint(distance, 45.0)
        val point2 = destinationPoint(distance, -135.0)
        return BoundingBox.fromGeoPoints(listOf(point1, point2))
    }
}