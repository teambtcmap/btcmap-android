package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** One option of a [RadioPickerContent]. */
data class RadioOption(
    val key: String,
    val label: String,
)

/**
 * A titled radio list, the shape shared by the map-style and verified-filter
 * pickers. Selecting an option reports its [RadioOption.key]; the host decides
 * whether to dismiss.
 */
@Composable
fun RadioPickerContent(
    options: List<RadioOption>,
    selectedKey: String?,
    onSelect: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option.key == selectedKey,
                        role = Role.RadioButton,
                    ) { onSelect(option.key) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option.key == selectedKey, onClick = null)
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
