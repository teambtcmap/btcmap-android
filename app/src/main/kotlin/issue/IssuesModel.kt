package issue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import area.AreasRepo
import area_element.AreaElementRepo
import element.ElementsRepo
import element.name
import json.toList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

class IssuesModel(
    private val areasRepo: AreasRepo,
    private val elementsRepo: ElementsRepo,
    private val areaElementRepo: AreaElementRepo,
    private val app: Application,
) : AndroidViewModel(app) {

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()

    fun setArgs(args: Args) {
        viewModelScope.launch {
            val area = areasRepo.selectById(args.areaId)!!

            val elements = areaElementRepo.selectByAreaId(area.id)
                .mapNotNull { elementsRepo.selectById(it.elementId) }

            val issues = elements.map { element ->
                val osmUrl =
                    "https://www.openstreetmap.org/${element.overpassData.getString("type")}/${
                        element.overpassData.getLong("id")
                    }"

                (element.tags.optJSONArray("issues") ?: JSONArray()).toList().map {
                    IssuesAdapter.Item(
                        type = it.getString("type"),
                        severity = it.getInt("severity"),
                        description = it.getString("description"),
                        osmUrl = osmUrl,
                        elementName = element.name(app.resources)
                    )
                }
            }.flatten()

            _state.update { State.ShowingItems(issues.sortedByDescending { it.severity }) }
        }
    }

    data class Args(val areaId: Long)

    sealed class State {

        data object Loading : State()

        data class ShowingItems(val items: List<IssuesAdapter.Item>) : State()
    }
}