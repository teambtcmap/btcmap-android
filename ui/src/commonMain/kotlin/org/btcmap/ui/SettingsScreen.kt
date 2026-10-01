package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * One row of a settings list.
 *
 * The labels are already-resolved strings; the screen never touches platform
 * resources, so the same list renders on Android and desktop.
 */
sealed interface SettingsItem {
    val key: String
    val title: String
    val secondary: String?

    /** A row that opens another screen or a dialog. */
    data class Action(
        override val key: String,
        override val title: String,
        override val secondary: String? = null,
    ) : SettingsItem

    /** A row with a switch on the trailing edge. */
    data class Toggle(
        override val key: String,
        override val title: String,
        override val secondary: String? = null,
        val checked: Boolean,
    ) : SettingsItem
}

@Composable
fun SettingsScreen(
    items: List<SettingsItem>,
    onItemClick: (key: String) -> Unit,
    onItemCheckedChange: (key: String, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items, key = { it.key }) { item ->
            SettingsRow(item, onItemClick, onItemCheckedChange)
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun SettingsRow(
    item: SettingsItem,
    onItemClick: (key: String) -> Unit,
    onItemCheckedChange: (key: String, checked: Boolean) -> Unit,
) {
    val supporting: (@Composable () -> Unit)? =
        item.secondary?.let { text -> { Text(text) } }

    val trailing: (@Composable () -> Unit)? = when (item) {
        is SettingsItem.Toggle -> {
            {
                Switch(
                    checked = item.checked,
                    onCheckedChange = { onItemCheckedChange(item.key, it) },
                )
            }
        }
        is SettingsItem.Action -> null
    }

    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = supporting,
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = item is SettingsItem.Action) {
            onItemClick(item.key)
        },
    )
}
