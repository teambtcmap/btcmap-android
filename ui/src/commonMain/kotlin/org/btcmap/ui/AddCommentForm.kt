package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** The comment length cap, matching the field's counter. */
const val COMMENT_MAX_LENGTH = 500

/** Test tags so the instrumented tests can drive the form. */
const val COMMENT_FIELD_TAG = "add-comment-field"
const val COMMENT_CONTINUE_TAG = "add-comment-continue"

data class AddCommentLabels(
    val disclosure: String,
    val currentFee: String,
    val comment: String,
    val placeholder: String,
    val continueLabel: String,
    val emptyComment: String,
    val failedToLoad: String,
    val tapToRetry: String,
)

data class AddCommentUiState(
    val quote: String?,
    val loadingQuote: Boolean,
    val quoteFailed: Boolean,
    val inputEnabled: Boolean,
    val actionsEnabled: Boolean,
    val ordering: Boolean,
    val showContinue: Boolean,
    val labels: AddCommentLabels,
)

/**
 * The add-comment form: the disclosure, the current fee, the comment field and
 * the continue button. The invoice block beneath it is still a Views include.
 */
@Composable
fun AddCommentForm(
    state: AddCommentUiState,
    onRetry: () -> Unit,
    onContinue: (comment: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var comment by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = state.labels.disclosure,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!state.quoteFailed) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.labels.currentFee,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = state.quote.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.loadingQuote) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }

        if (state.quoteFailed) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = state.labels.failedToLoad,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(state.labels.tapToRetry)
                }
            }
        }

        OutlinedTextField(
            value = comment,
            onValueChange = {
                if (it.length <= COMMENT_MAX_LENGTH) {
                    comment = it
                    // Clear the validation message as soon as the user starts
                    // fixing the input instead of leaving it pinned.
                    error = null
                }
            },
            enabled = state.inputEnabled,
            isError = error != null,
            label = { Text(state.labels.comment) },
            placeholder = { Text(state.labels.placeholder) },
            supportingText = {
                val current = error
                if (current != null) {
                    Text(current)
                } else {
                    Text("${comment.length}/$COMMENT_MAX_LENGTH")
                }
            },
            minLines = 5,
            maxLines = 10,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag(COMMENT_FIELD_TAG),
        )

        if (state.showContinue) {
            Button(
                onClick = {
                    val text = comment.trim()
                    if (text.isEmpty()) {
                        error = state.labels.emptyComment
                    } else {
                        onContinue(text)
                    }
                },
                enabled = state.actionsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
                    .testTag(COMMENT_CONTINUE_TAG),
            ) {
                Text(state.labels.continueLabel)
            }
        }

        if (state.ordering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .size(32.dp),
            )
        }
    }
}
