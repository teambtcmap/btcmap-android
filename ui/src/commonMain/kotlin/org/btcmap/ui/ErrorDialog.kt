package org.btcmap.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** The strings for a failure dialog. */
data class ErrorDialogLabels(
    val title: String,
    val ok: String,
)

/**
 * The dialog shown when an action fails (a submission, an edit). Unlike a
 * snackbar or a toast, it stays on screen until the user dismisses it, so a
 * message such as a geofence refusal can be read and screenshotted at leisure.
 */
@Composable
fun ErrorDialog(
    labels: ErrorDialogLabels,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(labels.ok) }
        },
    )
}
