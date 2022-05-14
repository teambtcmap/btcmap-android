package search

import androidx.lifecycle.ViewModel
import db.Place
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PlacesSearchResultModel : ViewModel() {

    val place = MutableStateFlow<Place?>(null)
}