package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.btcmap.util.rethrowIfCancellation

/** Test tag prefix for a saved item's delete button, keyed by the item id. */
const val SAVED_ITEMS_DELETE_TAG_PREFIX = "saved-items-delete-"

/** A saved place or area row: a name and a delete action. */
data class SavedItemUi(
    val id: Long,
    val name: String,
)

/** A saved-items screen's strings, so the screen stays resource-free. */
data class SavedItemsLabels(
    val empty: String,
    val delete: String,
)

/**
 * The signed-in user's saved places or saved areas, each a name and a delete
 * action. The list is loaded through [load] and deleted through [delete], so the
 * host owns the database and API work and the screen stays testable; a
 * successful delete reloads the list so it reflects any server-side change it
 * triggered. The screen carries no title of its own: the host shows "Saved
 * places" or "Saved areas" in its own title bar, and its back affordance leaves
 * this screen.
 *
 * The body is capped at [CONTENT_MAX_WIDTH] and centred, so a wide desktop or
 * tablet window keeps the rows readable instead of stretching them edge to edge
 * while leaving a phone unaffected.
 */
@Composable
fun SavedItemsScreen(
    labels: SavedItemsLabels,
    load: suspend () -> List<SavedItemUi>,
    delete: suspend (SavedItemUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var loaded by remember { mutableStateOf(false) }
    var savedItems by remember { mutableStateOf<List<SavedItemUi>>(emptyList()) }
    val deleting = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        savedItems = load()
        loaded = true
    }

    LaunchedEffect(Unit) { reload() }

    ContentColumn(modifier = modifier) {
        when {
            !loaded -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                CircularProgressIndicator()
            }

            savedItems.isEmpty() -> Text(
                text = labels.empty,
                modifier = Modifier.padding(16.dp),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(savedItems, key = { it.id }) { item ->
                    SavedItemRow(
                        item = item,
                        deleteDescription = labels.delete,
                        deleting = item.id in deleting,
                        onDelete = {
                            scope.launch {
                                deleting.add(item.id)
                                try {
                                    delete(item)
                                    reload()
                                } catch (t: Throwable) {
                                    // A failed delete leaves the row in place; the
                                    // exception is swallowed rather than escaping
                                    // the screen's coroutine.
                                    t.rethrowIfCancellation()
                                } finally {
                                    deleting.remove(item.id)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedItemRow(
    item: SavedItemUi,
    deleteDescription: String,
    deleting: Boolean,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDelete,
            enabled = !deleting,
            modifier = Modifier.testTag(SAVED_ITEMS_DELETE_TAG_PREFIX + item.id),
        ) {
            MaterialSymbol(glyph = "delete", contentDescription = deleteDescription)
        }
    }
}
