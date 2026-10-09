package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.btcmap.util.rethrowIfCancellation

/** The delete action's strings, so any toolbar can offer one. */
data class DeleteActionLabels(
    /** The action and the confirmation's confirm button. */
    val delete: String,
    val title: String,
    val message: String,
    /** Shown in the confirmation when the delete call fails. */
    val failed: String,
    /** The confirmation's dismiss button. */
    val cancel: String,
)

/**
 * A destructive delete action for a toolbar: a delete icon that opens a
 * confirmation and then runs [onDelete]. When [onDelete] is null — the signed-in
 * user may not delete this item — no action is drawn at all.
 *
 * [onDeleted] runs after a successful delete so the host can leave the screen. A
 * failure keeps the confirmation open with [DeleteActionLabels.failed] shown, so
 * a retry is one tap away. The test tags are optional so each host can key its
 * own.
 */
@Composable
fun DeleteAction(
    labels: DeleteActionLabels,
    onDelete: (suspend () -> Unit)?,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    deleteTag: String? = null,
    confirmTag: String? = null,
    errorTag: String? = null,
) {
    val delete = onDelete ?: return

    var confirming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    IconButton(
        onClick = {
            failed = false
            confirming = true
        },
        enabled = !deleting,
        modifier = modifier.then(if (deleteTag != null) Modifier.testTag(deleteTag) else Modifier),
    ) {
        MaterialSymbol(glyph = "delete", contentDescription = labels.delete)
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { if (!deleting) confirming = false },
            title = { Text(labels.title) },
            text = {
                Column {
                    Text(labels.message)
                    if (failed) {
                        Text(
                            text = labels.failed,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .then(
                                    if (errorTag != null) Modifier.testTag(errorTag) else Modifier,
                                ),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            deleting = true
                            failed = false
                            try {
                                delete()
                                confirming = false
                                onDeleted()
                            } catch (t: Throwable) {
                                t.rethrowIfCancellation()
                                failed = true
                            } finally {
                                deleting = false
                            }
                        }
                    },
                    enabled = !deleting,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = if (confirmTag != null) Modifier.testTag(confirmTag) else Modifier,
                ) {
                    if (deleting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(labels.delete)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirming = false },
                    enabled = !deleting,
                ) {
                    Text(labels.cancel)
                }
            },
        )
    }
}
