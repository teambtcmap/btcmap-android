package settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsViewModel(
    private val confRepository: ConfRepository
) : ViewModel() {

    fun getDistanceUnits() = confRepository.select().map { it.distanceUnits }

    suspend fun setDistanceUnits(value: String) {
        confRepository.upsert(confRepository.select().first().copy(distanceUnits = value))
    }
}