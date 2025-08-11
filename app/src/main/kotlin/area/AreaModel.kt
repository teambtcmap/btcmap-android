package area

import android.app.Application
import android.text.format.DateUtils
import android.util.TypedValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import area_element.AreaElementRepo
import element.ElementsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.R
import java.time.LocalDate

class AreaModel(
    private val areasRepo: AreasRepo,
    private val elementsRepo: ElementsRepo,
    private val areaElementRepo: AreaElementRepo,
    private val app: Application,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    fun setArgs(args: Args) {
        viewModelScope.launch {
            val area = areasRepo.selectById(args.areaId)!!

            val elements = areaElementRepo.selectByAreaId(area.id)
                .mapNotNull { elementsRepo.selectById(it.elementId) }
                .sortedByDescending { it.verifiedAt }
                .map {
                    val status: String
                    val colorResId: Int

                    if (it.verifiedAt != null) {
                        val date = DateUtils.getRelativeDateTimeString(
                            app,
                            LocalDate.parse(it.verifiedAt).toEpochDay() * 24 * 3_600 * 1_000,
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.WEEK_IN_MILLIS,
                            0,
                        ).split(",").first()

                        status = date
                        colorResId = com.google.android.material.R.attr.colorOnSurface
                    } else {
                        status = app.getString(R.string.not_verified)
                        colorResId = com.google.android.material.R.attr.colorOnErrorContainer
                    }

                    AreaAdapter.Item.Element(
                        id = it.id,
                        iconId = it.icon,
                        name = it.name,
                        status = status,
                        colorResId = colorResId,
                        showCheckmark = it.verifiedAt != null,
                        issues = 0,
                    )
                }.sortedBy { !it.showCheckmark }.toMutableList()

            val boundingBoxPaddingPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16f,
                app.resources.displayMetrics,
            ).toInt()

            val map = AreaAdapter.Item.Map(
                geoJson = area.tags["geo_json"].toString(),
                bounds = area.tags.bounds(),
                paddingPx = boundingBoxPaddingPx,
            )

            val contact = AreaAdapter.Item.Contact(
                website = area.tags["contact:website"]?.jsonPrimitive?.contentOrNull?.toHttpUrlOrNull(),
                twitter = area.tags["contact:twitter"]?.jsonPrimitive?.contentOrNull?.toHttpUrlOrNull(),
                telegram = area.tags["contact:telegram"]?.jsonPrimitive?.contentOrNull?.toHttpUrlOrNull(),
                discord = area.tags["contact:discord"]?.jsonPrimitive?.contentOrNull?.toHttpUrlOrNull(),
                youtube = area.tags["contact:youtube"]?.jsonPrimitive?.contentOrNull?.toHttpUrlOrNull(),
            )

            val issuesCount = elements.sumOf { it.issues }

            val items = buildList {
                add(map)

                if (area.tags.containsKey("description")) {
                    add(
                        AreaAdapter.Item.Description(
                            area.tags["description"]?.jsonPrimitive?.content ?: ""
                        )
                    )
                }

                add(contact)

                if (issuesCount > 0) {
                    add(AreaAdapter.Item.Issues(issuesCount))
                }
            } + elements

            _state.update {
                State.Loaded(
                    area = area,
                    items = items,
                )
            }
        }
    }

    suspend fun selectArea(id: Long): Area? {
        return areasRepo.selectById(id)
    }

    data class Args(
        val areaId: Long,
    )

    sealed class State {

        data object Loading : State()

        data class Loaded(
            val area: Area,
            val items: List<AreaAdapter.Item>,
        ) : State()
    }
}