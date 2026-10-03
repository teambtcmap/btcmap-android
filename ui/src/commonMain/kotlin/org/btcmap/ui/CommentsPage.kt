package org.btcmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.comment.CommentsAdapterItem
import org.btcmap.comment.commentDateFormatter
import org.btcmap.comment.toAdapterItem
import org.btcmap.db.Database

/** The comments screen's strings, so the screen stays resource-free. */
data class CommentsLabels(
    val add: String,
    val failedToLoad: String,
    val noComments: String,
)

/**
 * A place's comments: it runs [sync], reads the stored comments, and renders the
 * shared [CommentsScreen].
 *
 * The empty state is held back until the sync finishes and distinguishes a
 * genuinely empty list from one that could not be loaded. [reloadKey] re-runs
 * the sync and the read, e.g. after a comment was posted.
 */
@Composable
fun CommentsPage(
    db: Database,
    placeId: Long,
    labels: CommentsLabels,
    onAddComment: () -> Unit,
    sync: suspend () -> Boolean,
    reloadKey: Int = 0,
    modifier: Modifier = Modifier,
) {
    var items by remember { mutableStateOf<List<CommentsAdapterItem>>(emptyList()) }
    var emptyMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        emptyMessage = null
        val synced = sync()
        val loaded = withContext(Dispatchers.IO) {
            val formatter = commentDateFormatter()
            db.comment.selectByPlaceId(placeId).map { it.toAdapterItem(formatter) }
        }
        items = loaded
        emptyMessage = when {
            loaded.isNotEmpty() -> null
            synced -> labels.noComments
            else -> labels.failedToLoad
        }
    }

    CommentsScreen(
        items = items,
        emptyMessage = emptyMessage,
        addDescription = labels.add,
        onAddComment = onAddComment,
        modifier = modifier,
    )
}
