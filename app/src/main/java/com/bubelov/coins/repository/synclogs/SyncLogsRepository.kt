package com.bubelov.coins.repository.synclogs

import android.content.SharedPreferences
import com.bubelov.coins.PreferenceKeys
import com.bubelov.coins.model.SyncLogEntry
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class SyncLogsRepository @Inject constructor(
        private val preferences: SharedPreferences,
        private val gson: Gson
) {
    var syncLogs: List<SyncLogEntry>
        get() {
            val json = preferences.getString(PreferenceKeys.SYNC_LOGS, "")

            return when (json) {
                "" -> emptyList()
                else -> gson.fromJson(json, SyncLog::class.java).entries
            }
        }
        private set(value) {
            preferences.edit().putString(PreferenceKeys.SYNC_LOGS, gson.toJson(SyncLog(value))).apply()
        }

    fun addEntry(entry: SyncLogEntry) {
        val entries = mutableListOf<SyncLogEntry>().apply {
            addAll(syncLogs)
            add(entry)
        }

        syncLogs = entries
    }

    data class SyncLog(val entries: List<SyncLogEntry>)
}