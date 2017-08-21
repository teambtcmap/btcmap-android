package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.bubelov.coins.Constants
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.model.NotificationArea
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle

/**
 * @author Igor Bubelov
 */

class NotificationAreaViewModel(application: Application) : AndroidViewModel(application) {
    val areaRepository = lazy { Injector.appComponent.notificationAreaRepository() }

    var notificationArea: NotificationArea?
        get() {
            return areaRepository.value.notificationArea
        }
        set(value) {
            areaRepository.value.notificationArea = value
        }

    fun getDefaultNotificationArea(defaultCameraPosition: CameraPosition): NotificationArea {
        return NotificationArea(
                defaultCameraPosition.target.latitude,
                defaultCameraPosition.target.longitude,
                Constants.DEFAULT_NOTIFICATION_AREA_RADIUS_METERS
        )
    }

    fun getZoomLevel(circle: Circle): Int {
        val scale = circle.radius / 500
        return (16 - Math.log(scale) / Math.log(2.0)).toInt()
    }
}