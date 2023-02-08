package filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import element.ElementCategory
import element.ElementsRepo
import element.pluralDisplayString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FilterElementsModel(
    private val elementsRepo: ElementsRepo,
) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    private val filteredCategories = mutableListOf<String>()

    fun onViewCreated(filteredCategories: List<String>) {
        this.filteredCategories.clear()
        this.filteredCategories += filteredCategories

        viewModelScope.launch {
            _state.update {
                State.Loaded(
                    elementsRepo.selectCategories().map { it.toItem() },
                    filteredCategories,
                )
            }
        }
    }

    fun setVisible(categoryId: String, visible: Boolean) {
        if (visible && filteredCategories.contains(categoryId)) {
            filteredCategories -= categoryId
        }

        if (!visible && !filteredCategories.contains(categoryId)) {
            filteredCategories += categoryId
        }

        viewModelScope.launch {
            _state.update {
                State.Loaded(
                    elementsRepo.selectCategories().map { it.toItem() },
                    filteredCategories,
                )
            }
        }
    }

    sealed class State {

        object Loading : State()

        data class Loaded(
            val items: List<ElementCategoriesAdapter.Item>,
            val filteredCategories: List<String>,
        ) : State()
    }

    private fun ElementCategory.toItem(): ElementCategoriesAdapter.Item {
        return ElementCategoriesAdapter.Item(
            this.singular,
            "${this.pluralDisplayString()} (${this.elements})",
            !filteredCategories.contains(this.singular),
        )
    }
}