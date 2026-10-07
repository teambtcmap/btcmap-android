package org.btcmap.ui

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow

/** One option of a [RadioPickerContent]. */
data class RadioOption(
    val key: String,
    val label: String,
)

/**
 * A titled radio list, the shape shared by the map-style and verified-filter
 * pickers. Selecting an option reports its [RadioOption.key]; the host decides
 * whether to dismiss.
 *
 * The list is a single [selectableGroup], so assistive tech announces the
 * options as one mutually exclusive set, and each row is at least the M3
 * minimum interactive height. It scrolls if the dialog cannot fit every option.
 */
@Composable
fun RadioPickerContent(
    options: List<RadioOption>,
    selectedKey: String?,
    onSelect: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .selectableGroup()
            .verticalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            val interactionSource = remember { MutableInteractionSource() }
            // A mouse resting on a row should not paint a highlight across it;
            // press and focus feedback (and the touch ripple on Android) stay.
            val withoutHover = remember(interactionSource) {
                HoverlessInteractionSource(interactionSource)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // The M3 minimum interactive size, so the whole row is a
                    // comfortable target rather than only the radio glyph.
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = option.key == selectedKey,
                        interactionSource = withoutHover,
                        role = Role.RadioButton,
                    ) { onSelect(option.key) },
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

/**
 * A [MutableInteractionSource] that drops pointer-hover interactions, so the
 * ripple never draws its hover state layer without affecting press or focus.
 */
private class HoverlessInteractionSource(
    private val delegate: MutableInteractionSource,
) : MutableInteractionSource {
    override val interactions: Flow<Interaction> get() = delegate.interactions

    override suspend fun emit(interaction: Interaction) {
        if (!interaction.isHover()) delegate.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean =
        if (interaction.isHover()) true else delegate.tryEmit(interaction)
}

private fun Interaction.isHover(): Boolean =
    this is HoverInteraction.Enter || this is HoverInteraction.Exit
