package reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import db.toZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset

class ReportsModel(
    private val reportsRepo: ReportsRepo,
) : ViewModel() {

    val args = MutableStateFlow<Args?>(null)

    val data = MutableStateFlow<Data?>(null)

    init {
        viewModelScope.launch {
            args.collect { args ->
                if (args == null) {
                    return@collect
                } else {
                    data.update {
                        val reports = reportsRepo.selectByAreaId(args.areaId)

                        withContext(Dispatchers.IO) {
                            Data(
                                verifiedPlaces = reports.mapNotNull {
                                    if (it.tags.optLong("up_to_date_elements", -1) == -1L) {
                                        null
                                    } else {
                                        Pair(
                                            first = it.date.toString(),
                                            second = it.tags.getLong("up_to_date_elements"),
                                        )
                                    }
                                },
                                totalPlaces = reports.mapNotNull {
                                    if (it.tags.optLong("total_elements", -1) == -1L) {
                                        null
                                    } else {
                                        Pair(
                                            first = it.date.toString(),
                                            second = it.tags.getLong("total_elements"),
                                        )
                                    }
                                },
                                verifiedPlacesFraction = reports.mapNotNull {
                                    val upToDateElements =
                                        if (it.tags.optLong("up_to_date_elements", -1) == -1L) {
                                            return@mapNotNull null
                                        } else {
                                            it.tags.getLong("up_to_date_elements")
                                        }

                                    val totalElements =
                                        if (it.tags.optLong("total_elements", -1) == -1L) {
                                            return@mapNotNull null
                                        } else {
                                            it.tags.getLong("total_elements")
                                        }

                                    Pair(
                                        first = it.date.toString(),
                                        second = upToDateElements.toFloat() / totalElements.toFloat() * 100f
                                    )
                                },
                                daysSinceVerified = reports.mapNotNull {
                                    val avgVerificationDate =
                                        it.tags.optString("avg_verification_date")
                                            .ifBlank { return@mapNotNull null }
                                    Pair(
                                        first = it.date,
                                        second = Duration.between(
                                            avgVerificationDate.toZonedDateTime()!!,
                                            it.date.atStartOfDay(ZoneOffset.UTC),
                                        ).toDays(),
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    data class Args(val areaId: String)

    data class Data(
        val verifiedPlaces: List<Pair<String, Long>>,
        val totalPlaces: List<Pair<String, Long>>,
        val verifiedPlacesFraction: List<Pair<String, Float>>,
        val daysSinceVerified: List<Pair<LocalDate, Long>>,
    )
}