package issue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import area.AreasRepo
import area.polygons
import element.ElementsRepo
import element.name
import json.toList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import map.boundingBox
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

class IssuesModel(
    private val areasRepo: AreasRepo,
    private val elementsRepo: ElementsRepo,
    private val app: Application,
) : AndroidViewModel(app) {

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()

    fun setArgs(args: Args) {
        viewModelScope.launch {
            val area = areasRepo.selectById(args.areaId)!!

            val polygons = area.tags.polygons()
            val boundingBox = boundingBox(polygons)
            val geometryFactory = GeometryFactory()

            val elements = elementsRepo.selectByBoundingBox(
                minLat = boundingBox.getLatSouth(),
                maxLat = boundingBox.getLatNorth(),
                minLon = boundingBox.getLonWest(),
                maxLon = boundingBox.getLonEast(),
            ).filter { element ->
                polygons.any {
                    val coordinate = Coordinate(element.lon, element.lat)
                    it.contains(geometryFactory.createPoint(coordinate))
                }
            }

            val issues = elements.map { element ->
                val osmUrl = "https://www.openstreetmap.org/${element.osmType}/${element.osmId}"

                element.issues.toList().map {
                    IssuesAdapter.Item(
                        type = it.getString("type"),
                        severity = it.getInt("severity"),
                        description = it.getString("description"),
                        osmUrl = osmUrl,
                        elementName = element.osmTags.name(app.resources)
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