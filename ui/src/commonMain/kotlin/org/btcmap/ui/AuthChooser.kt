package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One option of the account chooser. */
data class AuthChooserOption(
    val key: String,
    val title: String,
    val subtitle: String,
)

/**
 * The "create account / sign in" chooser shown before a credential form.
 */
@Composable
fun AuthChooserContent(
    options: List<AuthChooserOption>,
    onSelect: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)) {
        options.forEachIndexed { index, option ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option.key) }
                    .padding(top = if (index == 0) 16.dp else 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
