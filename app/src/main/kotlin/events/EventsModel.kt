package events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import db.SelectAllNotDeletedEventsAsListItems
import elements.ElementsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.btcmap.R
import org.koin.android.annotation.KoinViewModel
import java.time.ZonedDateTime
import java.util.regex.Pattern

@KoinViewModel
class EventsModel(
    private val eventsRepo: EventsRepo,
    private val elementsRepo: ElementsRepo,
    private val app: Application,
) : AndroidViewModel(app) {

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val allNotDeleted = eventsRepo.selectAllNotDeletedAsListItems()

            val items = allNotDeleted.map {
                EventsAdapter.Item(
                    date = ZonedDateTime.parse(it.event_date),
                    type = it.event_type,
                    elementId = it.element_id,
                    elementName = it.element_name ?: app.getString(R.string.unnamed_place),
                    username = it.user_name ?: "",
                    tipLnurl = it.lnurl(),
                )
            }

            _state.update { State.ShowingItems(items) }
        }
    }

    suspend fun selectElementById(id: String) = elementsRepo.selectById(id)

    sealed class State {

        object Loading : State()

        data class ShowingItems(val items: List<EventsAdapter.Item>) : State()
    }

    private fun SelectAllNotDeletedEventsAsListItems.lnurl(): String {
        val description = user_description ?: ""
        val pattern = Pattern.compile("\\(lightning:[^)]*\\)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(description)
        val matchFound: Boolean = matcher.find()

        return if (matchFound) {
            matcher.group().trim('(', ')')
        } else {
            ""
        }
    }
}