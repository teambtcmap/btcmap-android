package com.bubelov.coins.repository.area

import android.content.SharedPreferences

import com.bubelov.coins.PreferenceKeys
import com.bubelov.coins.model.NotificationArea
import com.google.gson.Gson

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class NotificationAreaRepository @Inject constructor(
        private val preferences: SharedPreferences,
        private val gson: Gson
) {
    var notificationArea: NotificationArea?
        get() = gson.fromJson(preferences.getString(PreferenceKeys.NOTIFICATION_AREA, ""), NotificationArea::class.java)
        set(area) = preferences.edit().putString(PreferenceKeys.NOTIFICATION_AREA, gson.toJson(area)).apply()
}