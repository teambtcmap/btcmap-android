package org.btcmap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.btcmap.comment.CommentsAdapterItem

/**
 * The comments for a place: the list, an empty state, and the add button.
 */
@Composable
fun CommentsScreen(
    items: List<CommentsAdapterItem>,
    emptyMessage: String?,
    addDescription: String,
    onAddComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        if (items.isEmpty() && emptyMessage != null) {
            Text(
                text = emptyMessage,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Leave room for the add button so it never covers the last row.
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            items(items, key = { it.id }) { item ->
                CommentRow(item)
            }
        }

        FloatingActionButton(
            onClick = onAddComment,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Bottom),
                )
                .padding(24.dp)
                .semantics { contentDescription = addDescription },
        ) {
            MaterialSymbol(glyph = "add", contentDescription = null)
        }
    }
}
