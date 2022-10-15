package areas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
            val communities = areasRepo.selectAllNotDeleted().filter { it.type != "country" }.map {
                val areaCenter =
                    GeoPoint((it.min_lat + it.max_lat) / 2.0, (it.min_lon + it.max_lon) / 2.0)
                Pair(it, areaCenter.distanceToAsDouble(location) / 1000)
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
                    name = it.first.name,
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