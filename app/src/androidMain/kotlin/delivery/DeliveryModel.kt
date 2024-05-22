package delivery

import android.app.Application
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import element.Element
import element.ElementsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.btcmap.R
import org.osmdroid.util.GeoPoint
import java.text.NumberFormat

class DeliveryModel(
    private val app: Application,
    private val elementsRepo: ElementsRepo,
) : ViewModel() {

    companion object {
        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }

    private val args = MutableStateFlow<Args?>(null)

    private val _items = MutableStateFlow<List<DeliveryAdapter.Item>>(emptyList())
    val items = _items.asStateFlow()

    init {
        viewModelScope.launch {
            args.collectLatest { args ->
                if (args == null) {
                    return@collectLatest
                }

                val unsortedElements = elementsRepo.selectByOsmTagValue(
                    "delivery",
                    "yes",
                ) + elementsRepo.selectByOsmTagValue(
                    "delivery",
                    "only",
                )

                val sortedElements = unsortedElements.sortedBy {
                    getDistanceInMeters(
                        startLatitude = args.userLat,
                        startLongitude = args.userLon,
                        endLatitude = it.lat,
                        endLongitude = it.lon,
                    )
                }

                _items.update {
                    sortedElements.map {
                        it.toAdapterItem(
                            GeoPoint(
                                args.userLat,
                                args.userLon,
                            )
                        )
                    }
                }
            }
        }
    }

    fun setArgs(args: Args) {
        this.args.update { args }
    }

    private fun Element.toAdapterItem(userLocation: GeoPoint?): DeliveryAdapter.Item {
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

        return DeliveryAdapter.Item(
            element = this,
            icon = tags.optString("icon:android").ifBlank { "question_mark" },
            name = overpassData.getJSONObject("tags").optString("name").ifBlank { "Unnamed" },
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

    data class Args(
        val userLat: Double,
        val userLon: Double,
        val searchAreaId: Long,
    )
}