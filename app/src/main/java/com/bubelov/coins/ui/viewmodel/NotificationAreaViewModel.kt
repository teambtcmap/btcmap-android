/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.ui.viewmodel

import android.arch.lifecycle.ViewModel
import com.bubelov.coins.Constants
import com.bubelov.coins.model.NotificationArea
import com.bubelov.coins.repository.area.NotificationAreaRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import javax.inject.Inject

class NotificationAreaViewModel @Inject constructor(
    private var areaRepository: NotificationAreaRepository
) : ViewModel() {
    val notificationArea = areaRepository.notificationArea

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

    fun save(notificationArea: NotificationArea) {
        areaRepository.notificationArea = notificationArea
    }
}