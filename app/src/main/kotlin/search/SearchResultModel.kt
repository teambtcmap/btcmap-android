package search

import androidx.lifecycle.ViewModel
import elements.Element
import kotlinx.coroutines.flow.MutableStateFlow

class SearchResultModel : ViewModel() {

    val element = MutableStateFlow<Element?>(null)
}