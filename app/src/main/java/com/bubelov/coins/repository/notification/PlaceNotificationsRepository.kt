package com.bubelov.coins.repository.notification

import android.content.SharedPreferences
import com.bubelov.coins.PreferenceKeys

import com.bubelov.coins.model.PlaceNotification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlaceNotificationsRepository @Inject
internal constructor(private val preferences: SharedPreferences, private val gson: Gson) {
    var notifications: Collection<PlaceNotification>
        get() {
            val value = preferences.getString(PreferenceKeys.PLACE_NOTIFICATIONS, "[]")
            val typeToken = object : TypeToken<ArrayList<PlaceNotification>>() {}
            return gson.fromJson<MutableCollection<PlaceNotification>>(value, typeToken.type)
        }
        private set(places) = preferences.edit()
                .putString(PreferenceKeys.PLACE_NOTIFICATIONS, gson.toJson(places))
                .apply()

    fun addNotification(notification: PlaceNotification) {
        notifications += notification
    }

    fun clear() {
        preferences.edit().remove(PreferenceKeys.PLACE_NOTIFICATIONS).apply()
    }
}