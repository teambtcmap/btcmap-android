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

package com.bubelov.coins

import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.place.PlacesDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*
import javax.inject.Inject

class PlacesDbTests: BaseRobolectricTest() {
    @Inject lateinit var placesDb: PlacesDb

    private val cafe = Place(
            id = 1,
            name = "Coffee Corner",
            latitude = 50.0,
            longitude = 1.5,
            category = "cafe",
            description = "Best coffee in town!",
            currencies = arrayListOf("BTC", "ZEC"),
            openedClaims = 10,
            closedClaims = 1,
            phone = "12345",
            website = "https://foo.bar",
            openingHours = "7AM-5PM",
            visible = true,
            updatedAt = Date()
    )

    @Before
    fun setUp() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun isEmptyByDefault() {
        assertEquals(0, placesDb.count().blockingObserve())
    }

    @Test
    fun insertsPlace() {
        assertEquals(0, placesDb.count().blockingObserve())
        placesDb.insert(listOf(cafe))
        assertEquals(1, placesDb.count().blockingObserve())
        assertEquals(cafe, placesDb.find(cafe.id).blockingObserve())
        assertEquals(cafe, placesDb.all().blockingObserve()[0])
    }

    @Test
    fun searchIsWorking() {
        (1..100L).forEach {
            placesDb.insert(listOf(cafe.copy(id = it, description = cafe.description + it)))
        }

        assertEquals(100, placesDb.count().blockingObserve())
        assertEquals(1, placesDb.findBySearchQuery("55").size)
    }
}