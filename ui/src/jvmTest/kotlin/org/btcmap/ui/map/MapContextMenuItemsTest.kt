package org.btcmap.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The map's right-click menu rows: the add-location kinds the host offers, then
 * its copy-coordinates action.
 */
class MapContextMenuItemsTest {

    private val labels = AddLocationLabels(
        addPlace = "place-label",
        addEvent = "event-label",
        addNote = "note-label",
    )

    @Test
    fun copyCoordinates_isOfferedAfterTheAddLocationKinds() {
        val items = mapContextMenuItems(
            onAddPlace = {},
            onAddEvent = {},
            onAddNote = {},
            addLocationLabels = labels,
            onCopyCoordinates = {},
            copyCoordinatesLabel = "copy-label",
        )

        assertEquals(
            listOf("place-label", "event-label", "note-label", "copy-label"),
            items.map { it.label },
        )
        assertEquals(listOf("place", "event", "notes", "content_copy"), items.map { it.glyph })
    }

    @Test
    fun absentCopyAction_leavesTheAddLocationKinds() {
        val items = mapContextMenuItems(
            onAddPlace = {},
            onAddEvent = null,
            onAddNote = null,
            addLocationLabels = labels,
            onCopyCoordinates = null,
            copyCoordinatesLabel = "copy-label",
        )

        assertEquals(listOf("place-label"), items.map { it.label })
    }

    @Test
    fun copyRowInvokesItsOwnAction() {
        var copied = false
        val items = mapContextMenuItems(
            onAddPlace = null,
            onAddEvent = null,
            onAddNote = null,
            addLocationLabels = labels,
            onCopyCoordinates = { copied = true },
            copyCoordinatesLabel = "copy-label",
        )

        items.single().onClick()
        assertEquals(true, copied)
    }
}
