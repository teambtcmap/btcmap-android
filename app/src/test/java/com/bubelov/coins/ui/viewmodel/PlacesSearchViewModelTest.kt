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

import android.content.Context
import com.bubelov.coins.blockingObserve
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.placeicon.PlaceIconsRepository
import org.junit.Assert
import org.junit.Test
import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.graphics.Bitmap
import com.bubelov.coins.model.Place
import org.junit.Rule
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.util.*

class PlacesSearchViewModelTest {
    @JvmField @Rule val instantExecutor = InstantTaskExecutorRule()

    private val placesRepository = mock(PlacesRepository::class.java)
    private val placeIconsRepository = mock(PlaceIconsRepository::class.java)

    private val model: PlacesSearchViewModel

    init {
        model = PlacesSearchViewModel(mock(Context::class.java), placesRepository, placeIconsRepository)

        `when`(placesRepository.findBySearchQuery(ArgumentMatchers.anyString()))
            .thenReturn(listOf(
                generatePlace("Bar 1", "BTC"),
                generatePlace("Bar 2", "BTC"),
                generatePlace("Bar 3", "LTC")
            ))

        `when`(placeIconsRepository.getPlaceIcon(ArgumentMatchers.anyString()))
            .thenReturn(mock(Bitmap::class.java))
    }

    @Test
    fun searchBars() {
        model.init(null, "BTC")
        model.searchQuery.value = "bar"
        val results = model.searchResults.blockingObserve()
        verify(placesRepository).findBySearchQuery("bar")
        Assert.assertEquals(2, results.size)
        Assert.assertTrue(results.all { it.name.contains("bar", ignoreCase = true) })
    }

    @Test
    fun emptyOnShortQuery() {
        model.init(null, "BTC")
        model.searchQuery.value = "b"
        val results = model.searchResults.blockingObserve()
        verifyZeroInteractions(placesRepository)
        Assert.assertTrue(results.isEmpty())
    }

    @Test
    fun resetsLastSearch() {
        model.init(null, "BTC")
        model.searchQuery.value = "bar"
        model.searchQuery.value = ""
        val results = model.searchResults.blockingObserve()
        Assert.assertTrue(results.isEmpty())
    }

    private fun generatePlace(name: String, currency: String): Place {
        return Place(
            id = name.hashCode().toLong(),
            name = name,
            latitude = 0.0,
            longitude = 0.0,
            description = "",
            category = "",
            currencies = arrayListOf(currency),
            openedClaims = 0,
            closedClaims = 0,
            phone = "",
            website = "",
            visible = true,
            openingHours = "",
            updatedAt = Date(0)
        )
    }
}