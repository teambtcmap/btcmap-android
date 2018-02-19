package com.bubelov.coins.repository.area

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.content.SharedPreferences

import com.bubelov.coins.PreferenceKeys
import com.bubelov.coins.model.NotificationArea
import com.bubelov.coins.model.Preference
import com.bubelov.coins.repository.preference.PreferencesRepository
import com.google.gson.Gson
import org.jetbrains.anko.doAsync

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class NotificationAreaRepository @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val preferences: SharedPreferences,
    private val gson: Gson
) {
    init {
        if (preferences.contains(PreferenceKeys.NOTIFICATION_AREA)) {
            doAsync {
                preferencesRepository.insert(
                    Preference(
                        PreferenceKeys.NOTIFICATION_AREA,
                        preferences.getString(PreferenceKeys.NOTIFICATION_AREA, "")
                    )
                )
            }
        }
    }

    val notificationArea: LiveData<NotificationArea> =
        Transformations.map(
            preferencesRepository.find(PreferenceKeys.NOTIFICATION_AREA), { preference ->
                gson.fromJson(
                    preference?.value ?: "",
                    NotificationArea::class.java
                )
            })

    fun save(area: NotificationArea) {
        doAsync {
            preferencesRepository.insert(
                Preference(PreferenceKeys.NOTIFICATION_AREA, gson.toJson(area))
            )
        }
    }
}