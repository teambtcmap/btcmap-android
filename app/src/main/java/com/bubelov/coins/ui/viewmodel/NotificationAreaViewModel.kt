package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.bubelov.coins.Constants
import com.bubelov.coins.model.NotificationArea
import com.bubelov.coins.repository.area.NotificationAreaRepository
import com.bubelov.coins.util.appComponent
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class NotificationAreaViewModel(application: Application) : AndroidViewModel(application) {
    @Inject internal lateinit var areaRepository: NotificationAreaRepository

    var notificationArea: NotificationArea?
        get() {
            return areaRepository.notificationArea
        }
        set(value) {
            areaRepository.notificationArea = value
        }

    init {
        appComponent().inject(this)
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