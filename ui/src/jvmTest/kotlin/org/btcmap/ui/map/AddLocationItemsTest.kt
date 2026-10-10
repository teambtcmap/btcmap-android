package org.btcmap.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The add-location chooser's rows, shared by the search field's action and the
 * map's right-click context menu, so both present the same choices in the same
 * order.
 */
class AddLocationItemsTest {

    private val labels = AddLocationLabels(
        addPlace = "place-label",
        addEvent = "event-label",
        addNote = "note-label",
    )

    @Test
    fun allKinds_areOfferedInPlaceEventNoteOrder() {
        val items = addLocationItems(
            onAddPlace = {},
            onAddEvent = {},
            onAddNote = {},
            labels = labels,
        )
        assertEquals(listOf("place-label", "event-label", "note-label"), items.map { it.label })
        assertEquals(listOf("place", "event", "notes"), items.map { it.glyph })
    }

    @Test
    fun absentKinds_areLeftOut() {
        val items = addLocationItems(
            onAddPlace = null,
            onAddEvent = {},
            onAddNote = null,
            labels = labels,
        )
        assertEquals(listOf("event-label"), items.map { it.label })
    }

    @Test
    fun noKinds_offersNothing() {
        val items = addLocationItems(
            onAddPlace = null,
            onAddEvent = null,
            onAddNote = null,
            labels = labels,
        )
        assertEquals(emptyList(), items)
    }

    @Test
    fun rowInvokesItsOwnAction() {
        var placed = false
        val items = addLocationItems(
            onAddPlace = { placed = true },
            onAddEvent = null,
            onAddNote = null,
            labels = labels,
        )
        items.single().onClick()
        assertEquals(true, placed)
    }
}
