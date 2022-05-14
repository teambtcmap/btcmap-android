package search

import androidx.lifecycle.ViewModel
import db.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlacesSearchResultViewModel : ViewModel() {

    private val _place = MutableStateFlow<Place?>(null)
    val place = _place.asStateFlow()

    fun setPlace(place: Place) {
        _place.update { place }
    }

    fun consume() {
        _place.update { null }
    }
}