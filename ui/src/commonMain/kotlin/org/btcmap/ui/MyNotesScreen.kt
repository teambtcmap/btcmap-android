package org.btcmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.btcmap.util.rethrowIfCancellation

/** Test tag so a test can retry a failed load. */
const val MY_NOTES_RETRY_TAG = "my-notes-retry"

/** Test tag prefix for a note's private segment, keyed by the note id. */
const val MY_NOTES_PRIVATE_TAG_PREFIX = "my-notes-private-"

/** Test tag prefix for a note's public segment, keyed by the note id. */
const val MY_NOTES_PUBLIC_TAG_PREFIX = "my-notes-public-"

/** Test tag prefix for a note's delete button, keyed by the note id. */
const val MY_NOTES_DELETE_TAG_PREFIX = "my-notes-delete-"

/** The height of the map banner at the top of each note card. */
private val NOTE_CARD_MAP_HEIGHT = 140.dp

/** One of the signed-in user's notes, as the my-notes screen renders it. */
data class MyNoteUi(
    val id: Long,
    val text: String,
    val icon: String,
    val public: Boolean,
    val lat: Double,
    val lon: Double,
)

/** The my-notes screen's strings, so it stays resource-free. */
data class MyNotesLabels(
    val empty: String,
    val failed: String,
    val retry: String,
    /** The label beside a public note's switch. */
    val public: String,
    /** The label beside a private note's switch. */
    val private: String,
    val delete: String,
    val actionFailed: String,
    /** The accessible label of a note's map banner, which opens it on the map. */
    val openOnMap: String,
)

/**
 * The notes the signed-in user has added, each a card with a map banner at its
 * coordinate, its body, a switch for its visibility and a delete action. The list
 * is loaded through [load], the visibility changed through [update] and the note
 * deleted through [delete], so the host owns the API calls and the screen stays
 * testable; [map] renders the per-note banner, the host supplying it so a test
 * can render the list without a GPU. The screen carries no title of its own: the
 * host shows "My notes" in its own title bar.
 *
 * The body is capped at [CONTENT_MAX_WIDTH] and centred, so a wide desktop or
 * tablet window keeps the cards readable instead of stretching them edge to edge
 * while leaving a phone unaffected.
 */
@Composable
fun MyNotesScreen(
    labels: MyNotesLabels,
    load: suspend () -> List<MyNoteUi>,
    update: suspend (id: Long, public: Boolean) -> Unit,
    delete: suspend (MyNoteUi) -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable (note: MyNoteUi, modifier: Modifier) -> Unit = { _, _ -> },
    /** Opens a note on the full map, centred on its location. */
    onOpenOnMap: (MyNoteUi) -> Unit = {},
) {
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf<List<MyNoteUi>>(emptyList()) }
    var busyIds by remember { mutableStateOf(emptySet<Long>()) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loadError = null
        try {
            notes = load()
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            loadError = t.message ?: t.toString()
        } finally {
            loaded = true
        }
    }

    LaunchedEffect(Unit) { reload() }

    fun apply(id: Long, newPublic: Boolean) {
        actionError = null
        scope.launch {
            busyIds = busyIds + id
            try {
                update(id, newPublic)
                notes = notes.map { if (it.id == id) it.copy(public = newPublic) else it }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                actionError = labels.actionFailed
            } finally {
                busyIds = busyIds - id
            }
        }
    }

    fun remove(note: MyNoteUi) {
        actionError = null
        scope.launch {
            busyIds = busyIds + note.id
            try {
                delete(note)
                notes = notes.filterNot { it.id == note.id }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                actionError = labels.actionFailed
            } finally {
                busyIds = busyIds - note.id
            }
        }
    }

    ContentColumn(modifier = modifier) {
        actionError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        val error = loadError
        when {
            !loaded -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                CircularProgressIndicator()
            }

            error != null -> ErrorState(
                message = labels.failed,
                retry = labels.retry,
                onRetry = { scope.launch { reload() } },
                modifier = Modifier.weight(1f),
            )

            notes.isEmpty() -> EmptyState(message = labels.empty, modifier = Modifier.weight(1f))

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(notes, key = { it.id }) { note ->
                    MyNoteCard(
                        note = note,
                        labels = labels,
                        busy = note.id in busyIds,
                        onPublicChange = { apply(note.id, it) },
                        onDelete = { remove(note) },
                        map = map,
                        onOpenOnMap = { onOpenOnMap(note) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyNoteCard(
    note: MyNoteUi,
    labels: MyNotesLabels,
    busy: Boolean,
    onPublicChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    map: @Composable (note: MyNoteUi, modifier: Modifier) -> Unit,
    onOpenOnMap: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Full-width map banner; the card clips it to its rounded corners.
            // It is a button that opens the note on the full map, so a transparent
            // overlay catches the tap rather than the map's gesture layer.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NOTE_CARD_MAP_HEIGHT)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                map(note, Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClickLabel = labels.openOnMap) { onOpenOnMap() },
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    // The note's icon, so its pin kind is recognisable before the
                    // banner loads and while the banner is small.
                    MaterialSymbol(
                        glyph = note.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        text = note.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Footer: a private/public segmented control on the start, the
                // destructive delete on the end, so the two actions are separated
                // instead of being crammed into one trailing slot.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !note.public,
                            onClick = { onPublicChange(false) },
                            enabled = !busy,
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag(MY_NOTES_PRIVATE_TAG_PREFIX + note.id),
                        ) {
                            Text(labels.private)
                        }
                        SegmentedButton(
                            selected = note.public,
                            onClick = { onPublicChange(true) },
                            enabled = !busy,
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag(MY_NOTES_PUBLIC_TAG_PREFIX + note.id),
                        ) {
                            Text(labels.public)
                        }
                    }
                    TextButton(
                        onClick = onDelete,
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.testTag(MY_NOTES_DELETE_TAG_PREFIX + note.id),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Text(labels.delete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            MaterialSymbol(
                glyph = "notes",
                contentDescription = null,
                size = 48.sp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    retry: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            TextButton(
                onClick = onRetry,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(MY_NOTES_RETRY_TAG),
            ) {
                Text(retry)
            }
        }
    }
}
