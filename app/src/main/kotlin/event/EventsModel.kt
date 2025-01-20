package event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import element.ElementsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventsModel(
    private val eventsRepo: EventsRepo,
    private val elementsRepo: ElementsRepo,
    app: Application,
) : AndroidViewModel(app) {

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()

    private var limit = LIMIT

    init {
        loadItems()
    }

    suspend fun selectElementById(id: Long) = elementsRepo.selectById(id)

    fun onShowMoreItemsClick() {
        limit += LIMIT
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            val all = eventsRepo.selectAll(limit)

            val items = all.map {
                EventsAdapter.Item(
                    date = it.eventDate,
                    type = it.eventType,
                    elementId = it.elementId,
                    elementName = it.elementName.ifBlank {
                        it.tags.optString(
                            "element_name",
                            it.elementId.toString(),
                        )
                    },
                    username = it.userName,
                    tipLnurl = it.userTips,
                )
            }

            _state.update { State.ShowingItems(items) }
        }
    }

    sealed class State {

        data object Loading : State()

        data class ShowingItems(val items: List<EventsAdapter.Item>) : State()
    }

    companion object {
        private const val LIMIT = 100L
    }
}