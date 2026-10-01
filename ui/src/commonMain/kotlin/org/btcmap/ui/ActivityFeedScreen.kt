package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Test tag on the tappable retry shown after a failed load. */
const val FEED_RETRY_TAG = "feed-retry"

/** One activity feed row. */
data class ActivityFeedRow(
    val key: String,
    val icon: String,
    val placeName: String,
    val subtitle: String,
    val date: String,
)

sealed interface ActivityFeedState {
    data object Loading : ActivityFeedState

    /** Nothing to show: either genuinely empty, or a failure when [retryable]. */
    data class Empty(val message: String, val retryable: Boolean) : ActivityFeedState

    data class Content(val rows: List<ActivityFeedRow>) : ActivityFeedState
}

@Composable
fun ActivityFeedScreen(
    state: ActivityFeedState,
    onItemClick: (key: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when (state) {
            ActivityFeedState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            is ActivityFeedState.Empty -> Text(
                text = state.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
                    .then(
                        if (state.retryable) {
                            Modifier
                                .clickable { onRetry() }
                                .testTag(FEED_RETRY_TAG)
                        } else {
                            Modifier
                        }
                    ),
            )

            is ActivityFeedState.Content -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.rows, key = { it.key }) { row ->
                    ActivityFeedItemRow(row) { onItemClick(row.key) }
                }
            }
        }
    }
}

@Composable
private fun ActivityFeedItemRow(row: ActivityFeedRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaterialSymbol(
            glyph = row.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.placeName,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (row.subtitle.isNotEmpty()) {
                Text(
                    text = row.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = row.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
