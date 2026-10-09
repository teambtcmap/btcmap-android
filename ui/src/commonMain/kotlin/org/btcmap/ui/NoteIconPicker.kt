package org.btcmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** The diameter of one icon choice in an icon search's results. */
private val ICON_CHOICE_SIZE = 48.dp

/** How tall an icon search's results scroll before giving way to the form. */
private val ICON_RESULTS_HEIGHT = 200.dp

/**
 * The icon search shared by the add-note form and the note sheet's change-icon
 * dialog: a search field over every glyph the bundled font defines, of which
 * exactly one is selected. Nothing is offered until a query narrows the set.
 *
 * [searchTag] and [iconTagPrefix] are test tags, so each host can keep its own
 * (the add-note form and the sheet must not shadow each other's nodes).
 */
@Composable
internal fun NoteIconPicker(
    selectedIcon: String,
    onSelectIcon: (String) -> Unit,
    searchHint: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    searchTag: String? = null,
    iconTagPrefix: String? = null,
) {
    var iconQuery by rememberSaveable { mutableStateOf("") }

    // The icons matching what the user typed, searched across every glyph the
    // bundled font defines. Nothing is offered until a query narrows the set.
    val iconMatches = remember(iconQuery) {
        val query = iconQuery.trim()
        if (query.isEmpty()) {
            emptyList()
        } else {
            MATERIAL_SYMBOL_NAMES.filter { it.contains(query, ignoreCase = true) }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = iconQuery,
            onValueChange = { iconQuery = it },
            placeholder = { Text(searchHint) },
            singleLine = true,
            leadingIcon = { MaterialSymbol(glyph = "search", contentDescription = null) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (searchTag != null) Modifier.testTag(searchTag) else Modifier),
        )
        if (iconMatches.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = ICON_CHOICE_SIZE),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(ICON_RESULTS_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(iconMatches, key = { it }) { option ->
                    IconChoice(
                        icon = option,
                        selected = selectedIcon == option,
                        enabled = enabled,
                        onClick = { onSelectIcon(option) },
                        modifier = if (iconTagPrefix != null) {
                            Modifier.testTag(iconTagPrefix + option)
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}

/**
 * One icon in an icon search's results: a round, tappable glyph that is tinted
 * with the secondary container while it is the chosen one.
 */
@Composable
private fun IconChoice(
    icon: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(ICON_CHOICE_SIZE)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        MaterialSymbol(
            glyph = icon,
            contentDescription = icon,
            tint = content,
        )
    }
}
