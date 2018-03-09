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

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.bubelov.coins.any
import com.bubelov.coins.util.blockingObserve
import com.bubelov.coins.repository.Result
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.util.emptyPlace
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class EditPlaceViewModelTest {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    @Mock private lateinit var placesRepository: PlacesRepository

    init {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun submitNewPlace() {
        val model = EditPlaceViewModel(placesRepository)

        val place = emptyPlace().copy(
            id = 0,
            name = "Crypto Library"
        )

        `when`(placesRepository.addPlace(place)).thenReturn(Result.Success(place))

        model.init(place)
        model.submitChanges()

        Assert.assertTrue(model.submittedSuccessfully.blockingObserve())
        verify(placesRepository).addPlace(place)
        verify(placesRepository, never()).updatePlace(place)
    }

    @Test
    fun updateExistingPlace() {
        val model = EditPlaceViewModel(placesRepository)

        val place = emptyPlace().copy(
            id = 50,
            name = "Crypto Library"
        )

        `when`(placesRepository.updatePlace(place)).thenReturn(Result.Success(place))

        model.init(place)
        model.submitChanges()

        Assert.assertTrue(model.submittedSuccessfully.blockingObserve())
        verify(placesRepository).updatePlace(place)
        verify(placesRepository, never()).addPlace(place)
    }

    @Test
    fun handleFailure() {
        val model = EditPlaceViewModel(placesRepository)

        `when`(placesRepository.addPlace(any())).thenReturn(Result.Error(IllegalStateException("Test")))

        model.init(emptyPlace())
        model.submitChanges()
        Assert.assertEquals(false, model.submittedSuccessfully.blockingObserve())
    }
}