package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant
import org.btcmap.util.rethrowIfCancellation

/** Test tag for the note sheet's body text. */
const val NOTE_SHEET_TEXT_TAG = "note-sheet-text"

/** Test tag for the note sheet's visibility row. */
const val NOTE_SHEET_VISIBILITY_TAG = "note-sheet-visibility"

/** Test tag for the note sheet's creation row. */
const val NOTE_SHEET_CREATED_TAG = "note-sheet-created"

/** Test tags for the note sheet's delete action. */
const val NOTE_DELETE_TAG = "note-delete"
const val NOTE_DELETE_CONFIRM_TAG = "note-delete-confirm"
const val NOTE_DELETE_ERROR_TAG = "note-delete-error"

/** Test tags for the note sheet's edit action and its dialog. */
const val NOTE_EDIT_TAG = "note-edit"
const val NOTE_EDIT_FIELD_TAG = "note-edit-field"
const val NOTE_EDIT_SAVE_TAG = "note-edit-save"
const val NOTE_EDIT_ERROR_TAG = "note-edit-error"

/** Test tags for the note sheet's change-icon action and its dialog. */
const val NOTE_CHANGE_ICON_TAG = "note-change-icon"
const val NOTE_ICON_SEARCH_TAG = "note-icon-search"
const val NOTE_ICON_TAG_PREFIX = "note-icon-"
const val NOTE_ICON_SAVE_TAG = "note-icon-save"
const val NOTE_ICON_ERROR_TAG = "note-icon-error"

/** The note sheet's strings, so the map stays resource-free. */
data class NoteSheetLabels(
    val title: String,
    /** The visibility row's value while the note is public. */
    val public: String,
    /** The visibility row's value while the note is private. */
    val private: String,
    /** Formats the note's creation date, e.g. "Created Jan 1, 2025". */
    val created: (String) -> String,
    /** The edit action and its dialog (see [NoteEditDialog]). */
    val edit: String,
    val editTitle: String,
    val editFailed: String,
    val save: String,
    /** The change-icon action and its dialog (see [NoteIconDialog]). */
    val changeIcon: String,
    val iconSearchHint: String,
    val iconFailed: String,
    /** The delete action and its confirmation (see [DeleteAction]). */
    val delete: String,
    val deleteConfirmTitle: String,
    val deleteConfirmMessage: String,
    val deleteFailed: String,
    val cancel: String,
)

/**
 * The note details shown when a note pin is selected, the map's note sheet: the
 * pin's glyph and title, the note's body, and its visibility and creation date.
 *
 * It mirrors the place and event sheets, so a tapped note reads as a panel over
 * the map (rather than the modal dialog it used to be) and the map stays visible
 * behind it. [icon] is the glyph the pin carries, echoed in the header so the
 * sheet matches the pin that opened it.
 *
 * [onEditText] is null when the host does not offer editing, and then no pencil
 * is drawn; it edits the note's body. [onEditIcon] is likewise null when the
 * host does not offer changing the icon, and then the header glyph is inert;
 * it replaces the icon. [onDelete] is likewise null when the host does not
 * offer deletion. A successful delete runs [onDeleted] so the host can leave the
 * sheet and refresh the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSheet(
    text: String,
    icon: String,
    public: Boolean,
    createdAt: Instant,
    labels: NoteSheetLabels,
    onDismiss: () -> Unit,
    onEditText: (suspend (String) -> Unit)? = null,
    onEditIcon: (suspend (String) -> Unit)? = null,
    onDelete: (suspend () -> Unit)? = null,
    onDeleted: () -> Unit = onDismiss,
) {
    // Open at the half-expanded height the place and event sheets use, so the
    // map stays visible behind it; the user can drag it up to full screen.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    var editing by remember { mutableStateOf(false) }
    var editingIcon by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The map is the context the sheet is about, so keep it fully visible.
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
            ) {
                if (onEditIcon != null) {
                    IconButton(
                        onClick = { editingIcon = true },
                        modifier = Modifier.testTag(NOTE_CHANGE_ICON_TAG),
                    ) {
                        MaterialSymbol(
                            glyph = icon,
                            contentDescription = labels.changeIcon,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    MaterialSymbol(
                        glyph = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = labels.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                DeleteAction(
                    labels = DeleteActionLabels(
                        delete = labels.delete,
                        title = labels.deleteConfirmTitle,
                        message = labels.deleteConfirmMessage,
                        failed = labels.deleteFailed,
                        cancel = labels.cancel,
                    ),
                    onDelete = onDelete,
                    onDeleted = onDeleted,
                    deleteTag = NOTE_DELETE_TAG,
                    confirmTag = NOTE_DELETE_CONFIRM_TAG,
                    errorTag = NOTE_DELETE_ERROR_TAG,
                )
            }

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .testTag(NOTE_SHEET_TEXT_TAG),
                )
                if (onEditText != null) {
                    IconButton(
                        onClick = { editing = true },
                        modifier = Modifier.testTag(NOTE_EDIT_TAG),
                    ) {
                        MaterialSymbol(glyph = "edit", contentDescription = labels.edit)
                    }
                }
            }

            if (editing && onEditText != null) {
                NoteEditDialog(
                    initialText = text,
                    labels = labels,
                    onSave = onEditText,
                    onDismiss = { editing = false },
                )
            }

            if (editingIcon && onEditIcon != null) {
                NoteIconDialog(
                    initialIcon = icon,
                    labels = labels,
                    onSave = onEditIcon,
                    onDismiss = { editingIcon = false },
                )
            }

            NoteInfoRow(
                glyph = if (public) "public" else "lock",
                text = if (public) labels.public else labels.private,
                modifier = Modifier.testTag(NOTE_SHEET_VISIBILITY_TAG),
            )
            NoteInfoRow(
                glyph = "schedule",
                text = labels.created(createdAt.format(dateFormatter)),
                modifier = Modifier.testTag(NOTE_SHEET_CREATED_TAG),
            )
        }
    }
}

/** A glyph followed by a note detail, laid out like the place sheet's info rows. */
@Composable
private fun NoteInfoRow(
    glyph: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        MaterialSymbol(
            glyph = glyph,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * The note body editor opened from the sheet's pencil: a text field prefilled
 * with the note's body and a save action that runs [onSave]. A failure keeps the
 * dialog open with the error shown, so a retry is one tap away. The save is
 * disabled until the text actually changes, so tapping it cannot issue a no-op
 * request.
 */
@Composable
private fun NoteEditDialog(
    initialText: String,
    labels: NoteSheetLabels,
    onSave: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val trimmed = text.trim()

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(labels.editTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_NOTE_LENGTH) text = it },
                    minLines = 3,
                    maxLines = 8,
                    enabled = !saving,
                    label = { Text(labels.title) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NOTE_EDIT_FIELD_TAG),
                )
                if (failed) {
                    Text(
                        text = labels.editFailed,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag(NOTE_EDIT_ERROR_TAG),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        saving = true
                        failed = false
                        try {
                            onSave(trimmed)
                            onDismiss()
                        } catch (t: Throwable) {
                            t.rethrowIfCancellation()
                            failed = true
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving && trimmed.isNotEmpty() && trimmed != initialText.trim(),
                modifier = Modifier.testTag(NOTE_EDIT_SAVE_TAG),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(labels.save)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(labels.cancel)
            }
        },
    )
}

/**
 * The note icon picker opened from the sheet's header glyph: a search over the
 * note's icons with one selected, and a save action that runs [onSave]. A
 * failure keeps the dialog open with the error shown, so a retry is one tap
 * away. The save is disabled until the icon actually changes, so tapping it
 * cannot issue a no-op request.
 */
@Composable
private fun NoteIconDialog(
    initialIcon: String,
    labels: NoteSheetLabels,
    onSave: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var icon by remember(initialIcon) { mutableStateOf(initialIcon) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(labels.changeIcon) },
        text = {
            Column {
                NoteIconPicker(
                    selectedIcon = icon,
                    onSelectIcon = { icon = it },
                    searchHint = labels.iconSearchHint,
                    enabled = !saving,
                    searchTag = NOTE_ICON_SEARCH_TAG,
                    iconTagPrefix = NOTE_ICON_TAG_PREFIX,
                )
                if (failed) {
                    Text(
                        text = labels.iconFailed,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag(NOTE_ICON_ERROR_TAG),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        saving = true
                        failed = false
                        try {
                            onSave(icon)
                            onDismiss()
                        } catch (t: Throwable) {
                            t.rethrowIfCancellation()
                            failed = true
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving && icon != initialIcon,
                modifier = Modifier.testTag(NOTE_ICON_SAVE_TAG),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(labels.save)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(labels.cancel)
            }
        },
    )
}
