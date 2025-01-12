package area

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import map.boundingBox
import org.btcmap.R
import org.maplibre.android.geometry.LatLng
import java.text.NumberFormat

class AreasModel(
    private val areasRepo: AreasRepo,
    private val app: Application,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    fun setArgs(args: Args) {
        val location = LatLng(args.lat, args.lon)

        viewModelScope.launch {
            val communities = areasRepo
                .selectByType("community")
                .filter { it.tags.containsKey("icon:square") }
                .mapNotNull {
                    val polygons = runCatching {
                        it.tags.polygons()
                    }.getOrElse {
                        return@mapNotNull null
                    }

                    if (polygons.isEmpty()) {
                        return@mapNotNull null
                    }

                    val boundingBox = boundingBox(polygons)
                    Pair(it, boundingBox.center.distanceTo(location))
                }.sortedBy { it.second }

            val distanceFormat = NumberFormat.getNumberInstance().apply {
                maximumFractionDigits = 0
            }

            val items = communities.map {
                val distanceStringBuilder = StringBuilder()

                distanceStringBuilder.apply {
                    append(distanceFormat.format(it.second))
                    append(" ")
                    append(app.resources.getString(R.string.kilometers_short))
                }

                AreasAdapter.Item(
                    id = it.first.id,
                    iconUrl = it.first.tags["icon:square"]?.jsonPrimitive?.contentOrNull ?: "",
                    name = it.first.tags.name(),
                    distance = distanceStringBuilder.toString(),
                )
            }

            _state.update { State.Loaded(items) }
        }
    }

    data class Args(
        val lat: Double,
        val lon: Double,
    )

    sealed class State {

        object Loading : State()

        data class Loaded(val items: List<AreasAdapter.Item>) : State()
    }
}