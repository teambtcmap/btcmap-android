package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.ApiResult
import com.bubelov.coins.repository.place.PlacesRepository
import com.google.android.gms.maps.model.LatLng
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class EditPlaceViewModel(application: Application) : AndroidViewModel(application) {
    @Inject lateinit var placesRepository: PlacesRepository

    var place: Place? = null

    var pickedLocation: LatLng? = null

    private lateinit var callback: Callback

    fun init(place: Place?, callback: Callback) {
        Injector.appComponent.inject(this)
        this.place = place

        if (place != null) {
            pickedLocation = LatLng(place.latitude, place.longitude)
        }

        this.callback = callback
    }

    fun addPlace(place: Place) {
        callback.onTaskStarted()

        doAsync {
            val result = placesRepository.addPlace(place)

            uiThread {
                callback.onTaskStopped()

                when (result) {
                    is ApiResult.Success -> callback.onTaskSuccess(result.data)
                    is ApiResult.Error -> {
                        Timber.e(result.e)
                        callback.onTaskFailure()
                    }
                }
            }
        }
    }

    fun updatePlace(place: Place) {
        callback.onTaskStarted()

        doAsync {
            val result = placesRepository.updatePlace(place)

            uiThread {
                callback.onTaskStopped()

                when (result) {
                    is ApiResult.Success -> callback.onTaskSuccess(result.data)
                    is ApiResult.Error -> callback.onTaskFailure()
                }
            }
        }
    }

    interface Callback {
        fun onTaskStarted()
        fun onTaskStopped()
        fun onTaskSuccess(place: Place)
        fun onTaskFailure()
    }
}