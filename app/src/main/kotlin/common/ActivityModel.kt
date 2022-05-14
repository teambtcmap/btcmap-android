package common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import map.PlacesRepository

class ActivityModel(
    private val placesRepo: PlacesRepository,
) : ViewModel() {

    fun syncPlaces() {
        viewModelScope.launch {
            runCatching {
                placesRepo.sync()
            }.onFailure {
                Log.e(javaClass.simpleName, "Failed to sync places", it)
            }
        }
    }
}