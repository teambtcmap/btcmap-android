package org.btcmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** One customizable color: a label, its hex value and a swatch. */
data class ColorItem(
    val key: String,
    val title: String,
    val value: String,
    val color: Color,
)

@Composable
fun ColorSettingsScreen(
    items: List<ColorItem>,
    onItemClick: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items, key = { it.key }) { item ->
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = { Text(item.value) },
                trailingContent = { ColorSwatch(item.color) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onItemClick(item.key) },
            )
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

/**
 * A rounded swatch with a subtle outline, so light and dark colors stay visible
 * against any surface.
 */
@Composable
private fun ColorSwatch(color: Color) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    )
}
