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

package com.bubelov.coins.repository

import android.content.SharedPreferences
import com.bubelov.coins.model.SyncLogEntry
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.google.gson.Gson
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class SyncLogsRepositoryTest {
    @Mock private lateinit var sharedPreferences: SharedPreferences
    private lateinit var repository: SyncLogsRepository

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        repository = SyncLogsRepository(sharedPreferences, Gson())
    }

    @Test
    fun returnsEmptyListWithEmptyPreferences() {
        `when`(sharedPreferences.getString(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn("")

        Assert.assertTrue(repository.all().isEmpty())
        verify(sharedPreferences).getString(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        verifyNoMoreInteractions(sharedPreferences)
    }

    @Test
    fun savesNewEntry() {
        `when`(sharedPreferences.getString(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn("")

        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(editor.putString(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(editor)
        `when`(sharedPreferences.edit()).thenReturn(editor)

        val entry = SyncLogEntry(time = System.currentTimeMillis(), affectedPlaces = 5)
        repository.insert(entry)

        verify(sharedPreferences).edit()
        verify(editor).putString(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        verify(editor).apply()
    }
}