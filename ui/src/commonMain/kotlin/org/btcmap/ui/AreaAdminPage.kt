package org.btcmap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.btcmap.db.table.area.Area
import org.btcmap.i18n.getLocalizedName
import org.btcmap.util.rethrowIfCancellation
import kotlin.time.Clock

/** Test tags for the verify action's icon button and its in-flight spinner. */
const val AREA_ADMIN_VERIFY_BUTTON_TAG = "area-admin-verify"
const val AREA_ADMIN_VERIFY_PROGRESS_TAG = "area-admin-verify-progress"

/** The prefix of a field row's trailing action tag, suffixed with the label. */
const val AREA_ADMIN_ACTION_TAG_PREFIX = "area-admin-action-"

/** Test tag for the edit dialog's text field. */
const val AREA_ADMIN_EDIT_FIELD_TAG = "area-admin-edit-field"

/** The test tag of the action on the field labelled [label]. */
fun areaAdminActionTag(label: String): String = AREA_ADMIN_ACTION_TAG_PREFIX + label

/** An `IconButton`'s touch target, so the spinner keeps the icon's centre. */
private val VERIFY_ACTION_SIZE = 48.dp

/** The verify spinner's diameter, well inside the icon button's touch target. */
private val VERIFY_PROGRESS_SIZE = 24.dp

/**
 * The area admin page: one area's cached fields under the standard top bar, plus
 * a verify action in the bar and again on the verification-date field. Shared by
 * both hosts, the page owns the loading state, the in-flight spinner, the edit
 * dialog and the error dialog.
 *
 * [load] reads the area; [verify], [rename] and [updateDescription] perform the
 * server updates and persist them to the local cache, so the changes are not
 * lost on the next sync. [map] draws the preview (the host supplies the style
 * and marker colours).
 */
@Composable
fun AreaAdminPage(
    areaId: Long,
    labels: AppLabels,
    load: suspend () -> Area?,
    verify: suspend (id: Long, date: String) -> Unit,
    onBack: () -> Unit,
    map: @Composable (Area) -> Unit,
    onOpenUrl: (String) -> Unit = {},
    rename: (suspend (newName: String) -> Unit)? = null,
    updateDescription: (suspend (description: String?) -> Unit)? = null,
) {
    var area by remember { mutableStateOf<Area?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AreaAdminField?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(areaId) {
        area = load()
        loaded = true
    }

    val current = area

    fun verifyNow() {
        val target = current ?: return
        if (verifying) return
        scope.launch {
            verifying = true
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
                verify(target.id, today)
                // Reflect the new date at once; the host has already persisted
                // it, so a sync agrees.
                area = target.copy(verifiedAt = today)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                error = t
            } finally {
                verifying = false
            }
        }
    }

    ScreenPage(
        title = current?.getLocalizedName()?.ifBlank { null } ?: labels.manageAreasTitle,
        onBack = onBack,
        backContentDescription = labels.back,
        actions = {
            AreaAdminVerifyAction(
                verifying = verifying,
                enabled = current != null,
                verifyDescription = labels.verifyArea,
                onVerify = { verifyNow() },
            )
        },
    ) {
        when {
            !loaded -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }

            current != null -> AreaAdminScreen(
                area = current,
                map = { map(current) },
                onOpenUrl = onOpenUrl,
                fieldAction = { field ->
                    areaFieldAction(field, labels, rename, updateDescription, verifying)
                },
                onFieldAction = { field ->
                    when (field.kind) {
                        AreaAdminFieldKind.VerifiedAt -> verifyNow()
                        AreaAdminFieldKind.Name, AreaAdminFieldKind.Description -> editing = field
                        AreaAdminFieldKind.Plain -> Unit
                    }
                },
            )
        }
    }

    val fieldToEdit = editing
    if (fieldToEdit != null) {
        val spec = editSpecFor(fieldToEdit.kind, labels, current)
        if (spec != null) {
            AreaEditDialog(
                spec = spec,
                labels = labels,
                saving = saving,
                onSave = { newValue ->
                    scope.launch {
                        saving = true
                        try {
                            when (fieldToEdit.kind) {
                                AreaAdminFieldKind.Name -> {
                                    rename?.invoke(newValue)
                                    area = area?.copy(name = newValue)
                                }

                                AreaAdminFieldKind.Description -> {
                                    val value = newValue.ifEmpty { null }
                                    updateDescription?.invoke(value)
                                    area = area?.copy(description = value)
                                }

                                else -> Unit
                            }
                            editing = null
                        } catch (t: Throwable) {
                            t.rethrowIfCancellation()
                            editing = null
                            error = t
                        } finally {
                            saving = false
                        }
                    }
                },
                onDismiss = { if (!saving) editing = null },
            )
        }
    }

    error?.let { failure ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(labels.errorTitle) },
            text = { Text(failure.message ?: labels.errorMessage) },
            confirmButton = {
                TextButton(onClick = { error = null }) { Text(labels.ok) }
            },
        )
    }
}

/** The trailing action for a field, or null when the host cannot service it. */
private fun areaFieldAction(
    field: AreaAdminField,
    labels: AppLabels,
    rename: (suspend (String) -> Unit)?,
    updateDescription: (suspend (String?) -> Unit)?,
    verifying: Boolean,
): AreaFieldAction? = when (field.kind) {
    AreaAdminFieldKind.Name ->
        if (rename != null) AreaFieldAction("edit", labels.editName) else null

    AreaAdminFieldKind.Description ->
        if (updateDescription != null) AreaFieldAction("edit", labels.editDescription) else null

    AreaAdminFieldKind.VerifiedAt ->
        AreaFieldAction("verified", labels.verifyArea, enabled = !verifying)

    AreaAdminFieldKind.Plain -> null
}

/** The dialog spec for an editable field, or null for a field with no editor. */
private fun editSpecFor(
    kind: AreaAdminFieldKind,
    labels: AppLabels,
    area: Area?,
): EditDialogSpec? = when (kind) {
    AreaAdminFieldKind.Name -> EditDialogSpec(
        title = labels.editName,
        fieldLabel = labels.nameField,
        initial = area?.name.orEmpty(),
        singleLine = true,
        allowEmpty = false,
    )

    AreaAdminFieldKind.Description -> EditDialogSpec(
        title = labels.editDescription,
        fieldLabel = labels.descriptionField,
        initial = area?.description.orEmpty(),
        singleLine = false,
        allowEmpty = true,
    )

    else -> null
}

/** The shape of an edit dialog, resolved from the field and the host's labels. */
private data class EditDialogSpec(
    val title: String,
    val fieldLabel: String,
    val initial: String,
    val singleLine: Boolean,
    val allowEmpty: Boolean,
)

/**
 * The verify action: the verify icon, or a spinner in its place while a verify
 * is in flight. The spinner is centred in an [IconButton]-sized box so it does
 * not jump off the icon's centre.
 */
@Composable
internal fun AreaAdminVerifyAction(
    verifying: Boolean,
    enabled: Boolean,
    verifyDescription: String,
    onVerify: () -> Unit,
) {
    if (verifying) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(VERIFY_ACTION_SIZE),
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(VERIFY_PROGRESS_SIZE)
                    .testTag(AREA_ADMIN_VERIFY_PROGRESS_TAG),
            )
        }
    } else {
        IconButton(
            onClick = onVerify,
            enabled = enabled,
            modifier = Modifier.testTag(AREA_ADMIN_VERIFY_BUTTON_TAG),
        ) {
            MaterialSymbol(glyph = "verified", contentDescription = verifyDescription)
        }
    }
}

/**
 * The edit dialog: a name (single line) or description (multi-line) field, saved
 * through the host. Save is disabled while a save is in flight and until the
 * value actually changes; an empty description clears the field.
 */
@Composable
private fun AreaEditDialog(
    spec: EditDialogSpec,
    labels: AppLabels,
    saving: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(spec.initial) { mutableStateOf(spec.initial) }
    val trimmed = text.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = spec.singleLine,
                minLines = if (spec.singleLine) 1 else 3,
                enabled = !saving,
                label = { Text(spec.fieldLabel) },
                modifier = Modifier.testTag(AREA_ADMIN_EDIT_FIELD_TAG),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmed) },
                enabled = !saving &&
                    (spec.allowEmpty || trimmed.isNotEmpty()) &&
                    trimmed != spec.initial.trim(),
            ) {
                Text(labels.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(labels.cancel)
            }
        },
    )
}
