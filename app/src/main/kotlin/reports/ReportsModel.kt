package reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReportsModel(
    private val reportsRepo: ReportsRepo,
) : ViewModel() {

    val args = MutableStateFlow<Args?>(null)

    val reports = MutableStateFlow<List<Report>>(emptyList())

    init {
        viewModelScope.launch {
            args.collect { args ->
                if (args == null) {
                    return@collect
                } else {
                    reports.update { reportsRepo.selectByAreaId(args.areaId) }
                }
            }
        }
    }

    data class Args(val areaId: String)
}