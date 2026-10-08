package org.btcmap.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** The note dialog's strings, so the map stays resource-free. */
data class NoteDialogLabels(
    val title: String,
    val ok: String,
)

/**
 * A simple read-only dialog showing one note's text, opened by tapping the note's
 * pin on the map.
 */
@Composable
fun NoteDialog(
    text: String,
    labels: NoteDialogLabels,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(labels.ok)
            }
        },
    )
}
