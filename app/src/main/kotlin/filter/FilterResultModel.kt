package filter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class FilterResultModel : ViewModel() {

    val filteredCategories = MutableStateFlow<List<String>>(emptyList())
}