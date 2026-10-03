package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.btcmap.stats.StatsSection

/** Test tag for the stats card list, so a test can scroll to a section. */
const val STATS_LIST_TAG = "stats-list"

/** Test tag for one label/value row of a stats card. */
fun statsEntryTag(sectionKey: String, label: String): String = "stats-$sectionKey-$label"

/**
 * Renders a list of label/value cards, the shape shared by the database and
 * image stats screens.
 *
 * The theme and the icon font come from the surrounding [AppTheme]; the section
 * icons are omitted when no icon font is provided.
 */
@Composable
fun StatsScreen(
    sections: List<StatsSection>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(STATS_LIST_TAG),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sections, key = { it.key }) { section ->
            StatsCard(section)
        }
    }
}

@Composable
private fun StatsCard(section: StatsSection) {
    val iconFont = LocalIconFont.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = section.icon
                if (icon != null && iconFont != null) {
                    Text(
                        text = icon,
                        fontFamily = iconFont,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        // The ligature name is not a word; the icon is decorative.
                        modifier = Modifier
                            .size(24.dp)
                            .clearAndSetSemantics {},
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            section.entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
                Row(
                    // Label and value are one node, so a screen reader announces
                    // them together instead of as two stops.
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(statsEntryTag(section.key, entry.label))
                        .semantics(mergeDescendants = true) {}
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Text(
                        text = entry.value,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
