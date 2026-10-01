package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Test tags so the instrumented tests can drive the form. */
const val BOOST_CONTINUE_TAG = "boost-continue"
const val BOOST_OPTION_TAG_PREFIX = "boost-option-"

/** One selectable boost duration and the label it shows. */
data class BoostOption(
    val key: String,
    val label: String,
)

data class BoostFormUiState(
    val description: String,
    val durationTitle: String,
    val options: List<BoostOption>,
    val continueLabel: String,
    val selectedKey: String,
    val optionsEnabled: Boolean,
    val actionsEnabled: Boolean,
    val showContinue: Boolean,
)

/**
 * The boost form: the disclosure, the duration choices and the continue button.
 * The invoice block beneath it is a separate composable.
 */
@Composable
fun BoostForm(
    state: BoostFormUiState,
    onContinue: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(state.selectedKey) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = state.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = state.durationTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )

        Column(modifier = Modifier.padding(top = 4.dp)) {
            state.options.forEach { option ->
                val isSelected = selected == option.key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            enabled = state.optionsEnabled,
                            role = Role.RadioButton,
                        ) { selected = option.key }
                        .padding(vertical = 8.dp)
                        .testTag(BOOST_OPTION_TAG_PREFIX + option.key),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        if (state.showContinue) {
            Button(
                onClick = { onContinue(selected) },
                enabled = state.actionsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
                    .testTag(BOOST_CONTINUE_TAG),
            ) {
                Text(state.continueLabel)
            }
        }
    }
}
