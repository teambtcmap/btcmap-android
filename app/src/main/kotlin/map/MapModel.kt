package map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import db.Database
import db.Location
import location.UserLocationRepository
import db.Place
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

    private val _selectedPlace: MutableStateFlow<Place?> = MutableStateFlow(null)
    val selectedPlace = _selectedPlace.asStateFlow()

    private val _visiblePlaces = MutableStateFlow<List<PlaceWithMarker>>(emptyList())
    val visiblePlaces = _visiblePlaces.asStateFlow()

    private val _moveToLocation: MutableStateFlow<Location> = MutableStateFlow(UserLocationRepository.DEFAULT_LOCATION)
    val moveToLocation = _moveToLocation.asStateFlow()

    private val _syncMessage: MutableStateFlow<String> = MutableStateFlow("")
    val syncMessage = _syncMessage.asStateFlow()

    init {
        combine(_mapBoundingBox, db.placeQueries.selectCount().asFlow().mapToOne()) { viewport, _ ->
            withContext(Dispatchers.Default) {
                if (viewport == null) {
                    return@withContext
                }

                _visiblePlaces.update {
                    db.placeQueries.selectByBoundingBox(
                        minLat = min(viewport.latNorth, viewport.latSouth),
                        maxLat = max(viewport.latNorth, viewport.latSouth),
                        minLon = min(viewport.lonEast, viewport.lonWest),
                        maxLon = max(viewport.lonEast, viewport.lonWest),
                    )
                        .asFlow()
                        .mapToList()
                        .first()
                        .map { PlaceWithMarker(it, mapMarkersRepo.getMarker(it)) }
                }
            }
        }.launchIn(viewModelScope)

        locationRepo.requestLocationUpdates()

        viewModelScope.launch {
            val firstNonDefaultLocation =
                locationRepo.location.first { it != UserLocationRepository.DEFAULT_LOCATION }

            _moveToLocation.update { firstNonDefaultLocation }
        }

        viewModelScope.launch { syncPlaces() }
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

    fun selectPlace(id: String, moveToLocation: Boolean) {
        viewModelScope.launch {
            val place = db.placeQueries.selectById(id).asFlow().mapToOneOrNull().first()
            _selectedPlace.update { place }

            if (place != null && moveToLocation) {
                _moveToLocation.update { Location(place.lat, place.lon) }
            }
        }
    }

    fun invalidateMarkersCache() {
        mapMarkersRepo.invalidateCache()
    }

    private fun syncPlaces() {
        viewModelScope.launch {
            runCatching {
                val job = launch {
                    delay(1000)
                    _syncMessage.update { "Syncing places..." }
                }

                sync.sync()

                if (job.isCompleted) {
                    delay(1000)
                } else {
                    job.cancel()
                }

                _syncMessage.update { "" }
            }.onFailure {
                Log.e("sync", "Failed to sync places", it)
                _syncMessage.update { "Failed to sync places" }
                delay(5000)
                _syncMessage.update { "" }
            }
        }
    }
}