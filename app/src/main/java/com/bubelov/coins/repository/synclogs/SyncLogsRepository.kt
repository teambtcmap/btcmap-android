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

package com.bubelov.coins.repository.synclogs

import android.content.SharedPreferences
import com.bubelov.coins.PreferenceKeys
import com.bubelov.coins.model.SyncLogEntry
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLogsRepository @Inject constructor(
        private val preferences: SharedPreferences,
        private val gson: Gson
) {
    fun all(): List<SyncLogEntry> {
        val json = preferences.getString(PreferenceKeys.SYNC_LOGS, "")

        return when (json) {
            "" -> emptyList()
            else -> gson.fromJson(json, SyncLog::class.java).entries
        }
    }

    fun insert(entry: SyncLogEntry) {
        val entries = mutableListOf<SyncLogEntry>()
        entries += all()
        entries += entry

        preferences.edit().putString(
            PreferenceKeys.SYNC_LOGS,
            gson.toJson(SyncLog(entries))
        ).apply()
    }

    data class SyncLog(val entries: List<SyncLogEntry>)
}