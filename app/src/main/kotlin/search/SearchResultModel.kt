package search

import androidx.lifecycle.ViewModel
import db.table.place.Place
import kotlinx.coroutines.flow.MutableStateFlow

class SearchResultModel : ViewModel() {

    val element = MutableStateFlow<Place?>(null)
}