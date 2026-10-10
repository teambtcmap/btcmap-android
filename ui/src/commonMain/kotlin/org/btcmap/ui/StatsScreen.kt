package org.btcmap.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection

/** Test tag for the stats card list, so a test can scroll to a section. */
const val STATS_LIST_TAG = "stats-list"

/** Test tag for one label/value row of a stats card. */
fun statsEntryTag(sectionKey: String, label: String): String = "stats-$sectionKey-$label"

/**
 * Test tag for one detail row revealed beneath a stats entry, namespaced by the
 * parent entry's label since several entries may share a detail label (for
 * example every table has its own "Rows" detail).
 */
fun statsDetailTag(sectionKey: String, entryLabel: String, detailLabel: String): String =
    "stats-detail-$sectionKey-$entryLabel-$detailLabel"

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

/**
 * Renders the cards in an adaptive staggered grid: a phone shows one column,
 * while a wide window shows as many [minColumnWidth]-wide columns as fit. Each
 * column packs its cards independently, so a card taller than its neighbours
 * does not leave a hole beneath the shorter ones and the vertical space is
 * filled. Cards keep the same shape [StatsScreen] draws.
 */
@Composable
fun StatsGrid(
    sections: List<StatsSection>,
    modifier: Modifier = Modifier,
    minColumnWidth: Dp = 340.dp,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = minColumnWidth),
        modifier = modifier.fillMaxSize().testTag(STATS_LIST_TAG),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sections, key = { it.key }) { section ->
            StatsCard(section)
        }
    }
}

@Composable
private fun StatsCard(section: StatsSection) {
    val onClick = section.onClick
    if (onClick != null) {
        // The onClick overload keeps the indication inside the card's rounded
        // clip, so the hover/ripple highlight does not show square corners.
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            StatsCardContent(section)
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            StatsCardContent(section)
        }
    }
}

@Composable
private fun StatsCardContent(section: StatsSection) {
    val iconFont = LocalIconFont.current

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
            if (section.onClick != null && iconFont != null) {
                MaterialSymbol(
                    glyph = "chevron_right",
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        section.entries.forEachIndexed { index, entry ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
            key(entry.label) {
                StatsEntryRow(section = section, entry = entry)
            }
        }
    }
}

/**
 * One label/value row. A row whose [StatsEntry.details] are non-empty is
 * tappable: it shows an expand affordance and reveals the details beneath
 * itself, aligned to the same left edge as the row.
 */
@Composable
private fun StatsEntryRow(section: StatsSection, entry: StatsEntry) {
    val iconFont = LocalIconFont.current
    val expandable = entry.details.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            // Label and value are one node, so a screen reader announces
            // them together instead of as two stops. The whole row is the
            // toggle, so the click action sits on the same node as the text
            // rather than on the surrounding column (which also holds the
            // revealed details).
            modifier = Modifier
                .fillMaxWidth()
                .then(if (expandable) Modifier.clickable { expanded = !expanded } else Modifier)
                .testTag(statsEntryTag(section.key, entry.label))
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val entryIcon = entry.icon
            if (entryIcon != null && iconFont != null) {
                Text(
                    text = entryIcon,
                    fontFamily = iconFont,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // The ligature name is not a word; the icon is decorative.
                    modifier = Modifier
                        .size(20.dp)
                        .clearAndSetSemantics {},
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            )
            Text(
                text = entry.value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (expandable && iconFont != null) {
                Spacer(Modifier.width(8.dp))
                MaterialSymbol(
                    glyph = if (expanded) "expand_less" else "expand_more",
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 20.sp,
                )
            }
        }

        if (expanded) {
            entry.details.forEach { detail ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(statsDetailTag(section.key, entry.label, detail.label))
                        // Label and value are one node in the details too.
                        .semantics(mergeDescendants = true) {}
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The details keep the same left edge and colours as an
                    // unexpanded row, so a revealed step reads as one of them.
                    Text(
                        text = detail.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                    )
                    Text(
                        text = detail.value,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
