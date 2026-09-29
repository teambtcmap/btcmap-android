package org.btcmap.search

import org.junit.Assert
import org.junit.Test

class SearchAdapterDiffTest {

    private val callback = SearchAdapter.DiffCallback()

    private fun place(
        placeId: Long = 1L,
        name: String = "Bitcoin Cafe",
        distanceToUser: String? = "10 m",
        boosted: Boolean = false,
    ) = SearchAdapterItem.Place(
        placeId = placeId,
        icon = "local_cafe",
        name = name,
        distanceToUser = distanceToUser,
        boosted = boosted,
    )

    private fun area(
        areaId: Long = 1L,
        name: String = "Paris",
        distanceToUser: String? = "20 m",
    ) = SearchAdapterItem.Area(
        areaId = areaId,
        bbox = listOf(2.22, 48.81, 2.47, 48.91),
        iconUrl = null,
        headerImageUrl = null,
        icon = "public",
        name = name,
        distanceToUser = distanceToUser,
    )

    private fun event(
        eventId: Long = 1L,
        name: String = "Bitcoin Meetup",
        distanceToUser: String? = "30 m",
    ) = SearchAdapterItem.Event(
        eventId = eventId,
        icon = "event",
        name = name,
        distanceToUser = distanceToUser,
    )

    @Test
    fun areItemsTheSame_whenPlaceIdsMatch() {
        Assert.assertTrue(callback.areItemsTheSame(place(), place(distanceToUser = "99 m")))
    }

    @Test
    fun areItemsTheSame_whenPlaceIdsDiffer() {
        Assert.assertFalse(callback.areItemsTheSame(place(placeId = 1L), place(placeId = 2L)))
    }

    @Test
    fun areItemsTheSame_whenAreaIdsMatch() {
        Assert.assertTrue(callback.areItemsTheSame(area(), area(distanceToUser = "99 m")))
    }

    @Test
    fun areItemsTheSame_whenAreaIdsDiffer() {
        Assert.assertFalse(callback.areItemsTheSame(area(areaId = 1L), area(areaId = 2L)))
    }

    @Test
    fun areItemsTheSame_whenEventIdsMatch() {
        Assert.assertTrue(callback.areItemsTheSame(event(), event(distanceToUser = "99 m")))
    }

    @Test
    fun areItemsTheSame_whenEventIdsDiffer() {
        Assert.assertFalse(callback.areItemsTheSame(event(eventId = 1L), event(eventId = 2L)))
    }

    @Test
    fun areItemsTheSame_whenTypesDifferButIdsMatch() {
        // A place, an area and an event can share an id; they are still
        // different rows and must never be diffed as the same item.
        Assert.assertFalse(callback.areItemsTheSame(place(placeId = 1L), area(areaId = 1L)))
        Assert.assertFalse(callback.areItemsTheSame(area(areaId = 1L), event(eventId = 1L)))
        Assert.assertFalse(callback.areItemsTheSame(event(eventId = 1L), place(placeId = 1L)))
    }

    @Test
    fun areContentsTheSame_whenAllFieldsMatch() {
        Assert.assertTrue(callback.areContentsTheSame(place(), place()))
        Assert.assertTrue(callback.areContentsTheSame(area(), area()))
        Assert.assertTrue(callback.areContentsTheSame(event(), event()))
    }

    @Test
    fun areContentsTheSame_whenDistanceChanged() {
        Assert.assertFalse(callback.areContentsTheSame(place(), place(distanceToUser = "99 m")))
        Assert.assertFalse(callback.areContentsTheSame(area(), area(distanceToUser = "99 m")))
        Assert.assertFalse(callback.areContentsTheSame(event(), event(distanceToUser = "99 m")))
    }

    @Test
    fun areContentsTheSame_whenBoostChanged() {
        Assert.assertFalse(callback.areContentsTheSame(place(), place(boosted = true)))
    }
}
