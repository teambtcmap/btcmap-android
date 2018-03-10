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

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import com.bubelov.coins.repository.place.PlacesApi
import com.bubelov.coins.repository.place.PlacesAssetsCache
import com.bubelov.coins.repository.place.PlacesDb
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.util.Analytics
import com.bubelov.coins.util.emptyPlace
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class PlacesRepositoryTest {
    @JvmField @Rule val instantExecutor = InstantTaskExecutorRule()

    @Mock private lateinit var placesApi: PlacesApi
    @Mock private lateinit var placesDb: PlacesDb
    @Mock private lateinit var placesAssetsCache: PlacesAssetsCache
    @Mock private lateinit var analytics: Analytics

    init {
        MockitoAnnotations.initMocks(this)
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun usesAssetsCacheWhenEmpty() {
        `when`(placesDb.count()).thenReturn(MutableLiveData<Int>().apply { value = 0 })
        val places = listOf(emptyPlace().copy(id = 1, name = "Cafe"))
        `when`(placesAssetsCache.getPlaces()).thenReturn(places)

        PlacesRepository(placesApi, placesDb, placesAssetsCache, analytics)

        verify(placesAssetsCache).getPlaces()
        verify(placesDb).insert(places)
    }
}