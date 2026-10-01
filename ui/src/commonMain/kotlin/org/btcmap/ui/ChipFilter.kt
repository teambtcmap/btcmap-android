package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One selectable filter chip. */
data class ChipOption(
    val key: String,
    val label: String,
    val selected: Boolean,
)

/**
 * The activity feed filter: a wrapping group of area chips (multi-select) and a
 * single row of interval chips. The host reloads on each toggle; the dialog
 * stays open until confirmed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFilterContent(
    areasLabel: String,
    areaOptions: List<ChipOption>,
    intervalLabel: String,
    intervalOptions: List<ChipOption>,
    onAreaToggle: (key: String) -> Unit,
    onIntervalSelect: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        if (areaOptions.isNotEmpty()) {
            Text(
                text = areasLabel,
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                areaOptions.forEach { option ->
                    FilterChip(
                        selected = option.selected,
                        onClick = { onAreaToggle(option.key) },
                        label = { Text(option.label) },
                    )
                }
            }
        }

        Text(
            text = intervalLabel,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            intervalOptions.forEach { option ->
                FilterChip(
                    selected = option.selected,
                    onClick = { onIntervalSelect(option.key) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}
