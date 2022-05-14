package map

import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import db.Location
import location.UserLocationRepository
import db.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.math.max
import kotlin.math.min

@KoinViewModel
class MapModel(
    private val placesRepo: PlacesRepository,
    private val placeIconsRepo: PlaceIconsRepository,
    private val locationRepo: UserLocationRepository,
) : ViewModel() {

    val toast = MutableStateFlow("")

    private val _viewport: MutableStateFlow<BoundingBox?> = MutableStateFlow(null)
    val viewport = _viewport.asStateFlow()

    private val _selectedPlace: MutableStateFlow<Place?> = MutableStateFlow(null)
    val selectedPlace = _selectedPlace.asStateFlow()

    val userLocation = locationRepo.location

    private val _moveToLocation: MutableStateFlow<Location> = MutableStateFlow(UserLocationRepository.DEFAULT_LOCATION)
    val moveToLocation = _moveToLocation.asStateFlow()

    private val _visiblePlaces = MutableStateFlow<List<Pair<Place, BitmapDrawable>>>(emptyList())
    val visiblePlaces = _visiblePlaces.asStateFlow()

    fun syncPlaces() {
        viewModelScope.launch {
            runCatching {
                val job = launch {
                    delay(1000)
                    toast.update { "Syncing places..." }
                }

                placesRepo.sync()

                if (job.isCompleted) {
                    delay(1000)
                } else {
                    job.cancel()
                }

                toast.update { "" }
            }.onFailure {
                toast.update { "Failed to sync places" }
                delay(5000)
                toast.update { "" }
            }
        }
    }

    init {
        combine(_viewport, placesRepo.selectCount()) { viewport, _ ->
            withContext(Dispatchers.Default) {
                if (viewport == null) {
                    return@withContext
                }

                _visiblePlaces.update {
                    placesRepo.selectByBoundingBox(
                        minLat = min(viewport.latNorth, viewport.latSouth),
                        maxLat = max(viewport.latNorth, viewport.latSouth),
                        minLon = min(viewport.lonEast, viewport.lonWest),
                        maxLon = max(viewport.lonEast, viewport.lonWest),
                    ).first().map {
                        Pair(it, placeIconsRepo.getMarkerDrawable(it))
                    }
                }
            }
        }.launchIn(viewModelScope)

        locationRepo.requestLocationUpdates()

        viewModelScope.launch {
            val firstNonDefaultLocation =
                locationRepo.location.first { it != UserLocationRepository.DEFAULT_LOCATION }

            _moveToLocation.update { firstNonDefaultLocation }
        }
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
        _viewport.update { viewport }
    }

    fun selectPlace(id: Long, moveToLocation: Boolean) {
        viewModelScope.launch {
            val place = placesRepo.selectById(id).first()
            _selectedPlace.update { place }

            if (place != null && moveToLocation) {
                _moveToLocation.update { Location(place.lat, place.lon) }
            }
        }
    }
}