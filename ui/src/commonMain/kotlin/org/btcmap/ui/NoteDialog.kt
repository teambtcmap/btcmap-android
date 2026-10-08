package org.btcmap.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The note dialog's strings, so the map stays resource-free. */
data class NoteDialogLabels(
    val title: String,
    val ok: String,
)

/**
 * A simple read-only dialog showing one note's text, opened by tapping the note's
 * pin on the map. [icon] is the glyph its pin carries, echoed in the title so the
 * dialog matches the pin that opened it.
 */
@Composable
fun NoteDialog(
    text: String,
    icon: String,
    labels: NoteDialogLabels,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MaterialSymbol(
                    glyph = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(labels.title)
            }
        },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(labels.ok)
            }
        },
    )
}
