package reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import db.Report
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ReportsModel(
    private val reportsRepo: ReportsRepo,
) : ViewModel() {

    val reports = MutableStateFlow<List<Report>>(emptyList())

    init {
        viewModelScope.launch {
            reports.update { reportsRepo.selectByAreaId("") }
        }
    }
}