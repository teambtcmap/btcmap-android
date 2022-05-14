package map

import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import location.LocationRepository
import db.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import location.Location
import org.osmdroid.util.BoundingBox
import kotlin.math.max
import kotlin.math.min

class MapModel(
    private val placesRepository: PlacesRepository,
    private val placeIconsRepository: PlaceIconsRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    val location = locationRepository.location

    private val _moveToLocation: MutableStateFlow<Location> = MutableStateFlow(LocationRepository.DEFAULT_LOCATION)
    val moveToLocation = _moveToLocation.asStateFlow()

    private val _viewport: MutableStateFlow<BoundingBox?> = MutableStateFlow(null)
    val viewport = _viewport.asStateFlow()

    private val _visiblePlaces = MutableStateFlow<List<Pair<Place, BitmapDrawable>>>(emptyList())
    val visiblePlaces = _visiblePlaces.asStateFlow()

    private val _selectedPlace: MutableStateFlow<Place?> = MutableStateFlow(null)
    val selectedPlace = _selectedPlace.asStateFlow()

    init {
        combine(_viewport, placesRepository.selectCount()) { viewport, _ ->
            withContext(Dispatchers.Default) {
                if (viewport == null) {
                    return@withContext
                }

                _visiblePlaces.update {
                    placesRepository.selectByBoundingBox(
                        minLat = min(viewport.latNorth, viewport.latSouth),
                        maxLat = max(viewport.latNorth, viewport.latSouth),
                        minLon = min(viewport.lonEast, viewport.lonWest),
                        maxLon = max(viewport.lonEast, viewport.lonWest),
                    ).first().map {
                        Pair(it, placeIconsRepository.getMarkerDrawable(it))
                    }
                }
            }
        }.launchIn(viewModelScope)

        locationRepository.requestLocationUpdates()

        viewModelScope.launch {
            val firstNonDefaultLocation =
                locationRepository.location.first { it != LocationRepository.DEFAULT_LOCATION }

            _moveToLocation.update { firstNonDefaultLocation }
        }
    }

    fun onLocationPermissionGranted() {
        locationRepository.requestLocationUpdates()

        var found = false

        locationRepository.location.onEach { location ->
            if (!found && location != LocationRepository.DEFAULT_LOCATION) {
                found = true
                _moveToLocation.update { location }
            }
        }.launchIn(viewModelScope)
    }

    fun setMapViewport(viewport: BoundingBox) {
        _viewport.update { viewport }
    }

    suspend fun selectPlace(id: Long, moveToLocation: Boolean) {
        val place = placesRepository.selectById(id).first()
        _selectedPlace.update { place }
        
        if (place != null && moveToLocation) {
            _moveToLocation.update { Location(place.lat, place.lon) }
        }
    }
}