package map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import db.Database
import db.Element
import db.Location
import location.UserLocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.osmdroid.util.BoundingBox
import sync.Sync
import kotlin.math.max
import kotlin.math.min

@KoinViewModel
class MapModel(
    private val mapMarkersRepo: MapMarkersRepository,
    private val locationRepo: UserLocationRepository,
    private val sync: Sync,
    private val db: Database,
) : ViewModel() {

    val userLocation: StateFlow<Location> = locationRepo.location

    private val _mapBoundingBox: MutableStateFlow<BoundingBox?> = MutableStateFlow(null)
    val mapBoundingBox = _mapBoundingBox.asStateFlow()

    private val _selectedElement: MutableStateFlow<Element?> = MutableStateFlow(null)
    val selectedElement = _selectedElement.asStateFlow()

    private val _visibleElements = MutableStateFlow<List<ElementWithMarker>>(emptyList())
    val visibleElements = _visibleElements.asStateFlow()

    private val _moveToLocation: MutableStateFlow<Location> = MutableStateFlow(UserLocationRepository.DEFAULT_LOCATION)
    val moveToLocation = _moveToLocation.asStateFlow()

    private val _syncMessage: MutableStateFlow<String> = MutableStateFlow("")
    val syncMessage = _syncMessage.asStateFlow()

    init {
        combine(_mapBoundingBox, db.elementQueries.selectCount().asFlow().mapToOne()) { viewport, _ ->
            withContext(Dispatchers.Default) {
                if (viewport == null) {
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

        viewModelScope.launch {
            val firstNonDefaultLocation =
                locationRepo.location.first { it != UserLocationRepository.DEFAULT_LOCATION }

            _moveToLocation.update { firstNonDefaultLocation }
        }

        viewModelScope.launch { syncElements() }
    }

    fun onLocationPermissionGranted() {
        locationRepo.requestLocationUpdates()

        var found = false

        locationRepo.location.onEach { location ->
            if (!found && location != UserLocationRepository.DEFAULT_LOCATION) {
                found = true
                _moveToLocation.update { location }
            }
        }.launchIn(viewModelScope)
    }

    fun setMapViewport(viewport: BoundingBox) {
        _mapBoundingBox.update { viewport }
    }

    fun selectElement(id: String, moveToLocation: Boolean) {
        viewModelScope.launch {
            val element = db.elementQueries.selectById(id).asFlow().mapToOneOrNull().first()
            _selectedElement.update { element }

            if (element != null && moveToLocation) {
                _moveToLocation.update { Location(element.lat, element.lon) }
            }
        }
    }

    fun invalidateMarkersCache() {
        mapMarkersRepo.invalidateCache()
    }

    private fun syncElements() {
        viewModelScope.launch {
            runCatching {
                val job = launch {
                    delay(1000)
                    _syncMessage.update { "Syncing elements..." }
                }

                sync.sync()

                if (job.isCompleted) {
                    delay(1000)
                } else {
                    job.cancel()
                }

                _syncMessage.update { "" }
            }.onFailure {
                _syncMessage.update { "Failed to sync elements" }
                delay(5000)
                _syncMessage.update { "" }
            }
        }
    }
}