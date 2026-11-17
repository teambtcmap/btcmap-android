package org.btcmap.ui.map

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.btcmap.ui.MaterialSymbol

/** The labels of the map's right-click context menu. */
data class MapContextMenuLabels(
    val copyCoordinates: String,
)

/** One row of a map context menu: its label, its leading icon and its action. */
internal data class MapContextMenuItem(
    val label: String,
    val glyph: String,
    val onClick: () -> Unit,
)

/**
 * The map's right-click menu rows: the add-location kinds the host offers, in
 * their usual order, then its copy-coordinates action when it has one.
 */
internal fun mapContextMenuItems(
    onAddPlace: (() -> Unit)?,
    onAddEvent: (() -> Unit)?,
    onAddNote: (() -> Unit)?,
    addLocationLabels: AddLocationLabels,
    onCopyCoordinates: (() -> Unit)?,
    copyCoordinatesLabel: String,
): List<MapContextMenuItem> = buildList {
    addAll(addLocationItems(onAddPlace, onAddEvent, onAddNote, addLocationLabels))
    onCopyCoordinates?.let { copy ->
        add(MapContextMenuItem(copyCoordinatesLabel, "content_copy", copy))
    }
}

/**
 * A menu of actions anchored where the user invoked it, as a right-click does.
 * Shared by the search field's add action and the map's right-click context
 * menu, so both draw their rows the same way.
 */
@Composable
internal fun MapContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MapContextMenuItem>,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        items.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.label) },
                leadingIcon = { MaterialSymbol(glyph = item.glyph, contentDescription = null) },
                onClick = {
                    onDismissRequest()
                    item.onClick()
                },
            )
        }
    }
}
