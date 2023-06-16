package search

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import element.Element
import element.ElementsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*
import org.btcmap.R
import org.osmdroid.util.GeoPoint
import java.text.NumberFormat
import kotlin.system.measureTimeMillis

class SearchModel(
    private val app: Application,
    private val elementsRepo: ElementsRepo,
) : ViewModel() {

    companion object {
        private const val TAG = "search"

        private const val MIN_QUERY_LENGTH = 3

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }

    private val location = MutableStateFlow<GeoPoint?>(null)

    private val searchString = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<List<SearchAdapter.Item>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    init {
        combine(location, searchString) { location, searchString ->
            if (searchString.length < MIN_QUERY_LENGTH) {
                _searchResults.update { emptyList() }
            } else {
                var elements: List<Element>

                val queryTimeMillis = measureTimeMillis {
                    if (
                        searchString.equals("atm", ignoreCase = true) ||
                        searchString.equals("atms", ignoreCase = true) ||
                        searchString.equals(
                            app.getString(R.string.category_atm),
                            ignoreCase = true
                        ) ||
                        searchString.equals(
                            app.getString(R.string.category_atm_plural),
                            ignoreCase = true
                        )
                    ) {
                        elements = elementsRepo.selectByCategory("atm")
                    } else if (
                        searchString.equals("bar", ignoreCase = true) ||
                        searchString.equals("bars", ignoreCase = true) ||
                        searchString.equals(
                            app.getString(R.string.category_bar),
                            ignoreCase = true
                        ) ||
                        searchString.equals(
                            app.getString(R.string.category_bar_plural),
                            ignoreCase = true
                        )
                    ) {
                        elements = elementsRepo.selectByCategory("bar")
                    } else if (
                        searchString.equals("cafe", ignoreCase = true) ||
                        searchString.equals("cafes", ignoreCase = true) ||
                        searchString.equals(
                            app.getString(R.string.category_cafe),
                            ignoreCase = true
                        ) ||
                        searchString.equals(
                            app.getString(R.string.category_cafe_plural),
                            ignoreCase = true
                        )
                    ) {
                        elements = elementsRepo.selectByCategory("cafe")
                    } else if (
                        searchString.equals("hotel", ignoreCase = true) ||
                        searchString.equals("hotels", ignoreCase = true) ||
                        searchString.equals(
                            app.getString(R.string.category_hotel),
                            ignoreCase = true
                        ) ||
                        searchString.equals(
                            app.getString(R.string.category_hotel_plural),
                            ignoreCase = true
                        )
                    ) {
                        elements = elementsRepo.selectByCategory("hotel")
                    } else if (
                        searchString.equals("pub", ignoreCase = true) ||
                        searchString.equals("pubs", ignoreCase = true) ||
                        searchString.equals(
                            app.getString(R.string.category_pub),
                            ignoreCase = true
                        ) ||
                        searchString.equals(
                            app.getString(R.string.category_pub_plural),
                            ignoreCase = true
                        )
                    ) {
                        elements = elementsRepo.selectByCategory("pub")
                    } else if (
                        searchString.equals("restaurant", ignoreCase = true) ||
                        searchString.equals("restaurants", ignoreCase = true) ||
                        searchString.equals(
                            app.getString(R.string.category_restaurant),
                            ignoreCase = true
                        ) ||
                        searchString.equals(
                            app.getString(R.string.category_restaurant_plural),
                            ignoreCase = true
                        )
                    ) {
                        elements = elementsRepo.selectByCategory("restaurant")
                    } else {
                        elements = elementsRepo.selectBySearchString(searchString)
                    }
                }

                Log.d(TAG, "Search string: $searchString")
                Log.d(TAG, "Queried ${elements.size} elements in $queryTimeMillis ms")

                if (location != null) {
                    val sortTimeMillis = measureTimeMillis {
                        elements = elements.sortedBy {
                            getDistanceInMeters(
                                startLatitude = location.latitude,
                                startLongitude = location.longitude,
                                endLatitude = it.lat,
                                endLongitude = it.lon,
                            )
                        }
                    }

                    Log.d(TAG, "Sorted ${elements.size} elements by distance in $sortTimeMillis ms")
                }

                _searchResults.update { elements.map { it.toAdapterItem(location) } }
            }
        }.launchIn(viewModelScope)
    }

    fun setLocation(location: GeoPoint?) {
        this.location.update { location }
    }

    fun setSearchString(searchString: String) {
        this.searchString.update { searchString }
    }

    private fun Element.toAdapterItem(userLocation: GeoPoint?): SearchAdapter.Item {
        val distanceStringBuilder = StringBuilder()

        if (userLocation != null) {
            val elementLocation = GeoPoint(lat, lon)
            val distanceKm = userLocation.distanceToAsDouble(elementLocation) / 1000

            distanceStringBuilder.apply {
                append(DISTANCE_FORMAT.format(distanceKm))
                append(" ")
                append(app.resources.getString(R.string.kilometers_short))
            }
        }

        return SearchAdapter.Item(
            element = this,
            icon = tags["icon:android"]?.jsonPrimitive?.content ?: "question_mark",
            name = osmJson["tags"]!!.jsonObject["name"]?.jsonPrimitive?.contentOrNull ?: "Unnamed",
            distanceToUser = distanceStringBuilder.toString(),
        )
    }

    private fun getDistanceInMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Double {
        val distance = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance)
        return distance[0].toDouble()
    }
}