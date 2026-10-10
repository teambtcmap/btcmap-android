package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.btcmap.util.rethrowIfCancellation

/** The prefix of a roles dialog checkbox's tag, suffixed with the role id. */
const val USER_ADMIN_ROLE_TAG_PREFIX = "user-admin-role-"

/** The test tag of the checkbox for [role] in the roles dialog. */
fun userAdminRoleTag(role: String): String = USER_ADMIN_ROLE_TAG_PREFIX + role

/** The test tag of the geofence dialog's search field. */
const val USER_ADMIN_GEOFENCE_SEARCH_TAG = "user-admin-geofence-search"

/** The prefix of a geofence dialog checkbox's tag, suffixed with the area id. */
const val USER_ADMIN_GEOFENCE_AREA_TAG_PREFIX = "user-admin-geofence-area-"

/** The test tag of the checkbox for the area with [areaId] in the geofence dialog. */
fun userAdminGeofenceAreaTag(areaId: Long): String =
    USER_ADMIN_GEOFENCE_AREA_TAG_PREFIX + areaId

/** The label colour's alpha for a role the caller may not change. */
private const val DISABLED_ROLE_ALPHA = 0.38f

/** The tallest the geofence dialog's area list grows before it scrolls. */
private val GEOFENCE_LIST_MAX_HEIGHT = 360.dp

/** One area a geofence can reference, for the geofence editor. */
data class GeofenceAreaUi(
    val id: Long,
    val name: String,
    val type: String,
)

/**
 * The user admin page: one user's record under the standard top bar, whose roles
 * and geofence rows each carry their edit action. Shared by both hosts, the page
 * owns the roles and geofence dialogs, the in-flight saves and the error dialog;
 * [loadCaller] resolves the signed-in user whose roles decide what may be edited,
 * [loadAreaName] resolves a geofence area id to its cached name, [loadAreas]
 * supplies the areas the geofence editor offers, and [updateRoles] /
 * [updateGeofence] perform the server updates and return the updated record,
 * which the field list then shows.
 */
@Composable
fun UserAdminPage(
    user: ManageUserUi,
    labels: AppLabels,
    loadCaller: suspend () -> CallerRoles,
    loadAreaName: suspend (Long) -> String?,
    loadAreas: suspend () -> List<GeofenceAreaUi>,
    updateRoles: suspend (userId: Long, roles: List<String>) -> ManageUserUi,
    updateGeofence: suspend (userId: Long, geofence: List<Long>) -> ManageUserUi,
    onBack: () -> Unit,
) {
    var current by remember(user) { mutableStateOf(user) }
    var caller by remember { mutableStateOf<CallerRoles?>(null) }
    var geofenceLabels by remember { mutableStateOf<List<String>?>(null) }
    var editingRoles by remember { mutableStateOf(false) }
    var editingGeofence by remember { mutableStateOf(false) }
    var areas by remember { mutableStateOf<List<GeofenceAreaUi>?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(current.id) { caller = loadCaller() }

    // The geofence is a list of area ids; resolve each to its cached name, falling
    // back to the id when the cache has no such area.
    LaunchedEffect(current.geofence) {
        geofenceLabels = current.geofence.map { id -> loadAreaName(id) ?: id.toString() }
    }

    // The area list is only needed once the geofence dialog opens, so it is read
    // lazily rather than on every record view.
    LaunchedEffect(editingGeofence) {
        if (!editingGeofence || areas != null) return@LaunchedEffect
        try {
            areas = loadAreas()
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            error = t
            editingGeofence = false
        }
    }

    val edit = remember(caller, current) { userRolesEdit(caller, current) }
    val geofenceEditable = remember(caller, current) { canEditGeofence(caller, current) }

    ScreenPage(
        title = current.name,
        onBack = onBack,
        backContentDescription = labels.back,
    ) {
        UserAdminScreen(
            user = current,
            geofenceLabels = geofenceLabels,
            fieldAction = { field ->
                when {
                    field.kind == UserAdminFieldKind.Roles && edit.canEdit ->
                        UserFieldAction(icon = "edit", description = labels.editRoles)

                    field.kind == UserAdminFieldKind.Geofence && geofenceEditable ->
                        UserFieldAction(icon = "edit", description = labels.editGeofence)

                    else -> null
                }
            },
            onFieldAction = { field ->
                when (field.kind) {
                    UserAdminFieldKind.Roles -> editingRoles = true
                    UserAdminFieldKind.Geofence -> editingGeofence = true
                    UserAdminFieldKind.Plain -> Unit
                }
            },
        )
    }

    if (editingRoles) {
        RolesEditDialog(
            edit = edit,
            roleName = labels.roleName,
            title = labels.editRoles,
            save = labels.save,
            cancel = labels.cancel,
            saving = saving,
            onSave = { selected ->
                val roles = edit.toRoles(selected)
                scope.launch {
                    saving = true
                    try {
                        current = updateRoles(current.id, roles)
                        editingRoles = false
                    } catch (t: Throwable) {
                        t.rethrowIfCancellation()
                        error = t
                    } finally {
                        saving = false
                    }
                }
            },
            onDismiss = { if (!saving) editingRoles = false },
        )
    }

    val loadedAreas = areas
    if (editingGeofence && loadedAreas != null) {
        GeofenceEditDialog(
            areas = loadedAreas,
            initial = current.geofence.toSet(),
            labels = labels,
            saving = saving,
            onSave = { ids ->
                scope.launch {
                    saving = true
                    try {
                        current = updateGeofence(current.id, ids.toList())
                        editingGeofence = false
                    } catch (t: Throwable) {
                        t.rethrowIfCancellation()
                        error = t
                    } finally {
                        saving = false
                    }
                }
            },
            onDismiss = { if (!saving) editingGeofence = false },
        )
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

/**
 * The roles dialog: one checkbox per role. A role the caller may not change
 * stays visible but disabled (greyed), so the record's complete role set is
 * always shown; `root` is disabled for everyone because the API never lets it be
 * assigned. Save is enabled only once the selection actually differs.
 */
@Composable
private fun RolesEditDialog(
    edit: UserRolesEdit,
    roleName: (String) -> String,
    title: String,
    save: String,
    cancel: String,
    saving: Boolean,
    onSave: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(edit) { mutableStateOf(edit.initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ALL_USER_ROLES.forEach { role ->
                    val checked = role in selected
                    val enabled = role in edit.enabled && !saving
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                enabled = enabled,
                                role = Role.Checkbox,
                                onValueChange = { on ->
                                    selected = if (on) selected + role else selected - role
                                },
                            )
                            .testTag(userAdminRoleTag(role))
                            .padding(vertical = 4.dp),
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = roleName(role),
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (role in edit.enabled) 1f else DISABLED_ROLE_ALPHA,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selected) },
                enabled = !saving && selected != edit.initial,
            ) {
                Text(save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text(cancel) }
        },
    )
}

/**
 * The geofence dialog: a search over the cached areas with a checkbox each,
 * pre-selected with the user's current fence. Save is enabled only once the
 * selection actually differs. An area id in the fence the cache does not hold is
 * preserved (it is simply not shown).
 */
@Composable
private fun GeofenceEditDialog(
    areas: List<GeofenceAreaUi>,
    initial: Set<Long>,
    labels: AppLabels,
    saving: Boolean,
    onSave: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(initial) { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }

    val matches = remember(areas, query) {
        val needle = query.trim()
        if (needle.isEmpty()) areas
        else areas.filter { it.name.contains(needle, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.editGeofence) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { MaterialSymbol(glyph = "search", contentDescription = null) },
                    placeholder = { Text(labels.geofenceSearch) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(USER_ADMIN_GEOFENCE_SEARCH_TAG),
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (matches.isEmpty()) {
                    Text(
                        text = labels.geofenceNoMatches,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = GEOFENCE_LIST_MAX_HEIGHT),
                    ) {
                        items(matches, key = { it.id }) { area ->
                            val checked = area.id in selected
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = checked,
                                        enabled = !saving,
                                        role = Role.Checkbox,
                                        onValueChange = { on ->
                                            selected = if (on) {
                                                selected + area.id
                                            } else {
                                                selected - area.id
                                            }
                                        },
                                    )
                                    .testTag(userAdminGeofenceAreaTag(area.id))
                                    .padding(vertical = 4.dp),
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null, enabled = !saving)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = area.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = area.type.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selected) },
                enabled = !saving && selected != initial,
            ) {
                Text(labels.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text(labels.cancel) }
        },
    )
}
