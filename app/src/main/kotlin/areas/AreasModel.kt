package areas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import area.name
import area.polygons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import map.boundingBox
import org.btcmap.R
import org.osmdroid.util.GeoPoint
import java.text.NumberFormat

class AreasModel(
    private val areasRepo: AreasRepo,
    private val app: Application,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    fun setArgs(lat: Double, lon: Double) {
        val location = GeoPoint(lat, lon)

        viewModelScope.launch {
            val communities = areasRepo.selectAll().filter {
                it.tags["type"]?.jsonPrimitive?.content != "country"
            }.mapNotNull {
                val polygons = runCatching {
                    it.tags.polygons()
                }.getOrElse {
                    return@mapNotNull null
                }

                if (polygons.isEmpty()) {
                    return@mapNotNull null
                }

                val boundingBox = boundingBox(polygons)
                Pair(it, boundingBox.centerWithDateLine.distanceToAsDouble(location) / 1000)
            }.sortedBy { it.second }

            val items = communities.map {
                val distanceStringBuilder = StringBuilder()

                distanceStringBuilder.apply {
                    append(DISTANCE_FORMAT.format(it.second))
                    append(" ")
                    append(app.resources.getString(R.string.kilometers_short))
                }

                AreasAdapter.Item(
                    id = it.first.id,
                    iconUrl = it.first.tags["icon:square"]?.jsonPrimitive?.content ?: "",
                    name = it.first.tags.name(),
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