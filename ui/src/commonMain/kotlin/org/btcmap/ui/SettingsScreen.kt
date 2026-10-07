package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One entry of a settings list: a group [Header] or a row.
 *
 * The labels are already-resolved strings; the screen never touches platform
 * resources, so the same list renders on Android and desktop. [icon] is a
 * Material Symbols glyph name (not a localized string), so it lives in the
 * shared model next to the row it belongs to.
 */
sealed interface SettingsItem {
    val key: String
    val title: String
    val secondary: String?
    val icon: String?

    /** A non-interactive group label, naming the rows that follow it. */
    data class Header(
        override val key: String,
        override val title: String,
        override val secondary: String? = null,
        override val icon: String? = null,
    ) : SettingsItem

    /** A row that opens another screen or a dialog. */
    data class Action(
        override val key: String,
        override val title: String,
        override val secondary: String? = null,
        override val icon: String? = null,
    ) : SettingsItem

    /** A row with a switch on the trailing edge. */
    data class Toggle(
        override val key: String,
        override val title: String,
        override val secondary: String? = null,
        val checked: Boolean,
        override val icon: String? = null,
    ) : SettingsItem
}

/**
 * The settings list: the [items] in order, capped at [maxWidth] and centred so a
 * large desktop or tablet window does not stretch every row edge to edge (a
 * phone is narrower than the cap and is unaffected).
 */
@Composable
fun SettingsScreen(
    items: List<SettingsItem>,
    onItemClick: (String) -> Unit,
    onItemCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = CONTENT_MAX_WIDTH,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                // A hairline between two rows; the group headers already
                // separate the groups, so none is drawn under a header.
                if (
                    index > 0 &&
                    items[index - 1] !is SettingsItem.Header &&
                    item !is SettingsItem.Header
                ) {
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
                when (item) {
                    is SettingsItem.Header -> SettingsHeader(item)
                    else -> SettingsRow(item, onItemClick, onItemCheckedChange)
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun SettingsHeader(item: SettingsItem.Header) {
    Text(
        text = item.title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    item: SettingsItem,
    onItemClick: (String) -> Unit,
    onItemCheckedChange: (String, Boolean) -> Unit,
) {
    val leading: (@Composable () -> Unit)? = item.icon?.let { glyph ->
        {
            MaterialSymbol(
                glyph = glyph,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val supporting: (@Composable () -> Unit)? =
        item.secondary?.let { text -> { Text(text) } }

    val trailing: (@Composable () -> Unit)? = when (item) {
        is SettingsItem.Toggle -> {
            {
                // The row owns the toggle (see below), so the switch itself is
                // display-only; a null callback keeps it from handling its own
                // clicks and leaving two toggle targets for one row.
                Switch(checked = item.checked, onCheckedChange = null)
            }
        }
        // A navigable row reads as tappable rather than as plain text next to
        // the switch rows.
        is SettingsItem.Action -> {
            {
                MaterialSymbol(
                    glyph = "chevron_right",
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is SettingsItem.Header -> null
    }

    // The whole row toggles for a switch row and opens for an action row, so the
    // label is a target too, not only the trailing control.
    val modifier = when (item) {
        is SettingsItem.Toggle -> Modifier.toggleable(
            value = item.checked,
            role = Role.Switch,
            onValueChange = { onItemCheckedChange(item.key, it) },
        )
        is SettingsItem.Action -> Modifier.clickable { onItemClick(item.key) }
        is SettingsItem.Header -> Modifier
    }

    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = supporting,
        leadingContent = leading,
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier,
    )
}
