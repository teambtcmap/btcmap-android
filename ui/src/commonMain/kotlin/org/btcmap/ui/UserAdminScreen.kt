package org.btcmap.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The placeholder shown for a field the record has no value for. */
private const val USER_ADMIN_NOT_SET = "—"

/**
 * The roles the roles dialog lists, in display order. The server's `Role` enum
 * also carries `places_source` and `dashboard`, which are not managed here;
 * they are preserved on save (see [UserRolesEdit.hidden]).
 */
val ALL_USER_ROLES = listOf("user", "event_manager", "area_manager", "admin", "root")

/** The signed-in user, whose roles decide which role checkboxes are editable. */
data class CallerRoles(
    val id: Long,
    val roles: List<String>,
)

/**
 * What a roles dialog may change for one target, following the server's
 * `authorize_update` policy:
 *
 * - Root may edit any non-root user's roles (never assigning `root`) but never
 *   its own roles or another root's record.
 * - Admin may only add or remove `event_manager` / `area_manager` for a
 *   non-admin, non-root other user; it may never touch an admin or a root, nor
 *   change its own roles.
 *
 * [enabled] holds the editable roles; [hidden] holds the target's roles outside
 * [ALL_USER_ROLES], which the dialog does not show and always preserves.
 */
data class UserRolesEdit(
    val initial: Set<String>,
    val enabled: Set<String>,
    val hidden: Set<String>,
) {
    /** Whether any role may be changed at all. */
    val canEdit: Boolean get() = enabled.isNotEmpty()

    /** The roles to send for the chosen set: the shown selection plus preserved hidden roles. */
    fun toRoles(selected: Set<String>): List<String> =
        ALL_USER_ROLES.filter { it in selected } + hidden
}

/** The roles a [caller] may change on [target], or an empty edit when none. */
fun userRolesEdit(caller: CallerRoles?, target: ManageUserUi): UserRolesEdit {
    val callerRoles = caller?.roles.orEmpty()
    val isRoot = callerRoles.contains("root")
    val isAdmin = callerRoles.contains("admin")
    val isSelf = caller?.id == target.id
    val targetIsRoot = target.roles.contains("root")
    val targetIsAdmin = target.roles.contains("admin")

    val enabled = when {
        // Root: any non-root other user, but `root` itself is never assignable.
        isRoot && !isSelf && !targetIsRoot ->
            setOf("user", "event_manager", "area_manager", "admin")

        // Admin: only the manager roles, and only for a non-admin, non-root other.
        isAdmin && !isRoot && !isSelf && !targetIsRoot && !targetIsAdmin ->
            setOf("event_manager", "area_manager")

        else -> emptySet()
    }

    return UserRolesEdit(
        initial = target.roles.filter { it in ALL_USER_ROLES }.toSet(),
        enabled = enabled,
        hidden = target.roles.filterNot { it in ALL_USER_ROLES }.toSet(),
    )
}

/**
 * Whether [caller] may change [target]'s geofence, following the server's
 * `authorize_update` policy: any non-root (root may also edit its own), or — for
 * an admin — its own or a non-admin, non-root other user's. No one may touch
 * another admin's or another root's geofence.
 */
fun canEditGeofence(caller: CallerRoles?, target: ManageUserUi): Boolean {
    val callerRoles = caller?.roles.orEmpty()
    val isRoot = callerRoles.contains("root")
    val isAdmin = callerRoles.contains("admin")
    val isSelf = caller?.id == target.id
    val targetIsRoot = target.roles.contains("root")
    val targetIsAdmin = target.roles.contains("admin")

    return when {
        isRoot -> !targetIsRoot || isSelf
        isAdmin -> !targetIsRoot && (!targetIsAdmin || isSelf)
        else -> false
    }
}

/** The prefix of a field row's trailing action tag, suffixed with the label. */
const val USER_ADMIN_ACTION_TAG_PREFIX = "user-admin-action-"

/** The test tag of the action on the field labelled [label]. */
fun userAdminActionTag(label: String): String = USER_ADMIN_ACTION_TAG_PREFIX + label

/** The field's role, so the host can offer the right trailing action. */
enum class UserAdminFieldKind { Plain, Roles, Geofence }

/** One field of a user's record. */
data class UserAdminField(
    val label: String,
    val value: String,
    /** The Material Symbols glyph shown before the field. */
    val icon: String,
    val kind: UserAdminFieldKind = UserAdminFieldKind.Plain,
)

/** The trailing action a field offers: its icon and accessibility label. */
data class UserFieldAction(
    val icon: String,
    val description: String,
)

/**
 * The user admin screen: every field the search returned for one user, so an
 * admin can inspect the record. The host shows the user's name in its own bar
 * and passes the field rows' trailing action through [fieldAction] /
 * [onFieldAction] — the roles row carries its edit action, the rest none.
 *
 * [geofenceLabels] is the geofence's area names, resolved by the host from the
 * cache and aligned with `user.geofence`; while it is null the raw ids are shown.
 *
 * The body is a single column capped at [CONTENT_MAX_WIDTH] and centred, so a
 * desktop or tablet window does not stretch the values edge to edge. The field
 * labels are the API names and are deliberately not localized, like the area
 * admin screen's.
 */
@Composable
fun UserAdminScreen(
    user: ManageUserUi,
    modifier: Modifier = Modifier,
    geofenceLabels: List<String>? = null,
    fieldAction: ((UserAdminField) -> UserFieldAction?)? = null,
    onFieldAction: ((UserAdminField) -> Unit)? = null,
) {
    val fields = remember(user, geofenceLabels) { userAdminFields(user, geofenceLabels) }

    ContentColumn(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(fields, key = { it.label }) { field ->
                UserAdminFieldRow(field, fieldAction, onFieldAction)
            }
        }
    }
}

/**
 * One field row. A plain row is announced as a single label/value stop for a
 * screen reader; a field with an action keeps its own focusable node rather than
 * merging into the row.
 */
@Composable
private fun UserAdminFieldRow(
    field: UserAdminField,
    fieldAction: ((UserAdminField) -> UserFieldAction?)?,
    onFieldAction: ((UserAdminField) -> Unit)?,
) {
    val action = fieldAction?.invoke(field)
    val handler = onFieldAction

    ListItem(
        overlineContent = { Text(field.label) },
        headlineContent = { Text(field.value) },
        leadingContent = {
            MaterialSymbol(glyph = field.icon, contentDescription = null)
        },
        trailingContent = if (action != null && handler != null) {
            {
                IconButton(
                    onClick = { handler(field) },
                    modifier = Modifier.testTag(userAdminActionTag(field.label)),
                ) {
                    MaterialSymbol(glyph = action.icon, contentDescription = action.description)
                }
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (action == null) {
            Modifier.semantics(mergeDescendants = true) {}
        } else {
            Modifier
        },
    )
}

/**
 * Every field of [user], in the order the API returns them. [geofenceLabels] are
 * the geofence area names, aligned with `user.geofence`; null falls back to the
 * raw ids.
 */
internal fun userAdminFields(
    user: ManageUserUi,
    geofenceLabels: List<String>? = null,
): List<UserAdminField> = listOf(
    UserAdminField("id", user.id.toString(), icon = "fingerprint"),
    UserAdminField("name", user.name, icon = "person"),
    UserAdminField(
        "roles",
        user.roles.joinToString(", ").ifBlank { USER_ADMIN_NOT_SET },
        icon = "badge",
        kind = UserAdminFieldKind.Roles,
    ),
    UserAdminField("created_at", user.createdAt.ifBlank { USER_ADMIN_NOT_SET }, icon = "event"),
    UserAdminField(
        "geofence",
        (geofenceLabels ?: user.geofence.map { it.toString() })
            .joinToString(", ")
            .ifBlank { USER_ADMIN_NOT_SET },
        icon = "location_on",
        kind = UserAdminFieldKind.Geofence,
    ),
    UserAdminField("npub", user.npub ?: USER_ADMIN_NOT_SET, icon = "key"),
)
