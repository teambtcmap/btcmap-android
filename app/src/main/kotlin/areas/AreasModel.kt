package areas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.R
import org.koin.android.annotation.KoinViewModel
import org.osmdroid.util.GeoPoint
import java.text.NumberFormat

@KoinViewModel
class AreasModel(
    private val areasRepo: AreasRepo,
    private val app: Application,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    fun setArgs(lat: Double, lon: Double) {
        val location = GeoPoint(lat, lon)

        viewModelScope.launch {
            val communities = areasRepo.selectAllNotDeleted().filter {
                val tags: JsonObject = Json.decodeFromString(it.tags)
                tags["type"]?.jsonPrimitive?.content != "country"
            }.map {
                val tags: JsonObject = Json.decodeFromString(it.tags)

                val minLat = tags["box:south"]!!.jsonPrimitive.double
                val maxLat = tags["box:north"]!!.jsonPrimitive.double
                val minLon = tags["box:west"]!!.jsonPrimitive.double
                val maxLon = tags["box:east"]!!.jsonPrimitive.double

                val areaCenter = GeoPoint((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0)
                Pair(it, areaCenter.distanceToAsDouble(location) / 1000)
            }.sortedBy { it.second }

            val items = communities.map {
                val distanceStringBuilder = StringBuilder()

                distanceStringBuilder.apply {
                    append(DISTANCE_FORMAT.format(it.second))
                    append(" ")
                    append(app.resources.getString(R.string.kilometers_short))
                }

                val tags: JsonObject = Json.decodeFromString(it.first.tags)

                AreasAdapter.Item(
                    id = it.first.id,
                    iconUrl = tags["icon:square"]?.jsonPrimitive?.content ?: "",
                    name = tags["name"]?.jsonPrimitive?.content
                        ?: app.getString(R.string.unnamed_area),
                    distance = distanceStringBuilder.toString(),
                )
            }

            _state.update { State.ShowingItems(items) }
        }
    }

    sealed class State {

        object Loading : State()

        data class ShowingItems(val items: List<AreasAdapter.Item>) : State()
    }

    companion object {
        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }
}