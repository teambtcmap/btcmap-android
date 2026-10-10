package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.btcmap.api.UserSearchResult
import org.btcmap.util.rethrowIfCancellation

/** The test tag of the manage-users search field. */
const val MANAGE_USERS_SEARCH_TAG = "manage-users-search"

/** How long the screen waits after the last keystroke before searching. */
private const val USER_SEARCH_DEBOUNCE_MILLIS = 300L

/** One user on the manage-users screen; also the record the detail screen shows. */
data class ManageUserUi(
    val id: Long,
    val name: String,
    val roles: List<String>,
    /** The RFC 3339 account creation timestamp, formatted by the host. */
    val createdAt: String,
    /**
     * Area IDs the user is restricted to when acting as an event manager. Empty
     * means unrestricted.
     */
    val geofence: List<Long> = emptyList(),
    /** Bech32 npub of the linked Nostr identity, or null when none is linked. */
    val npub: String? = null,
)

/** The row/record model for a user the API returned. */
fun UserSearchResult.toManageUserUi(): ManageUserUi = ManageUserUi(
    id = id,
    name = name,
    roles = roles,
    createdAt = createdAt,
    geofence = geofence,
    npub = npub,
)

/** The manage-users screen's strings, so the screen stays resource-free. */
data class ManageUsersLabels(
    val search: String,
    val clear: String,
    /** Shown before anything is typed. */
    val prompt: String,
    val noMatches: String,
    val failed: String,
    val retry: String,
    /** Formats an account's creation timestamp, e.g. "Joined %1$s". */
    val created: (String) -> String,
)

/** The manage-users screen's body state. */
private sealed interface ManageUsersState {
    data object Idle : ManageUsersState
    data object Failed : ManageUsersState
    data class Loaded(val users: List<ManageUserUi>) : ManageUsersState
}

/**
 * The "manage users" screen: a search field over the users the API returns for
 * the typed query, so an admin or root can look one up. The search runs [search]
 * (debounced, so a burst of keystrokes issues one request), so the host owns the
 * API call and the screen stays testable. Only admins and roots reach it: the
 * settings row is hidden for everyone else, and the server enforces the role on
 * the search itself.
 *
 * The screen carries no title of its own: the host shows "Manage users" in its
 * own bar, and its back affordance leaves this screen.
 */
@Composable
fun ManageUsersScreen(
    labels: ManageUsersLabels,
    search: suspend (String) -> List<ManageUserUi>,
    onUserClick: (ManageUserUi) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<ManageUsersState>(ManageUsersState.Idle) }
    var searching by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(query, reloadKey) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            searching = false
            state = ManageUsersState.Idle
            return@LaunchedEffect
        }

        searching = true
        // Each keystroke restarts this effect, so the delay debounces the burst
        // and only the query the user stops on reaches the API.
        delay(USER_SEARCH_DEBOUNCE_MILLIS)
        try {
            state = ManageUsersState.Loaded(search(trimmed))
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            state = ManageUsersState.Failed
        } finally {
            searching = false
        }
    }

    ContentColumn(modifier = modifier) {
        ManageUsersSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = labels.search,
            clearDescription = labels.clear,
        )

        when (val current = state) {
            ManageUsersState.Idle ->
                if (searching) {
                    ManageUsersProgress(modifier = Modifier.weight(1f))
                } else {
                    ManageUsersStateMessage(
                        icon = "manage_search",
                        text = labels.prompt,
                        modifier = Modifier.weight(1f),
                    )
                }

            ManageUsersState.Failed ->
                if (searching) {
                    ManageUsersProgress(modifier = Modifier.weight(1f))
                } else {
                    ManageUsersStateMessage(
                        icon = "error",
                        text = labels.failed,
                        modifier = Modifier.weight(1f),
                        action = {
                            TextButton(onClick = { reloadKey++ }) { Text(labels.retry) }
                        },
                    )
                }

            is ManageUsersState.Loaded ->
                if (current.users.isEmpty()) {
                    ManageUsersStateMessage(
                        icon = "search_off",
                        text = labels.noMatches,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        items(current.users, key = { it.id }) { user ->
                            ManageUserRow(
                                user = user,
                                labels = labels,
                                onClick = { onUserClick(user) },
                            )
                        }
                    }
                }
        }
    }
}

/**
 * One user: the person glyph centred against the name, roles and creation date,
 * with a trailing chevron showing the row opens the record. Tapping it calls
 * [onClick].
 *
 * A plain [Row] rather than a [ListItem]: when the supporting slot measures as
 * more than one line — which the two supporting lines here always do — material3
 * switches the item to its three-line layout and top-aligns the leading content,
 * leaving the icon visibly high. Centring the row's children keeps the icon
 * level with the whole text block, and the 16/12 dp spacing and type styles
 * follow the list item tokens.
 */
@Composable
private fun ManageUserRow(
    user: ManageUserUi,
    labels: ManageUsersLabels,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        MaterialSymbol(
            glyph = "person",
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            user.rolesText()?.let { roles ->
                Text(
                    text = roles,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = labels.created(user.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        MaterialSymbol(
            glyph = "chevron_right",
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The roles shown under a user's name, or null when the user has none. */
private fun ManageUserUi.rolesText(): String? =
    roles.takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { role ->
            role.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }

/** A loading spinner for a search in flight, matching the other list screens. */
@Composable
private fun ManageUsersProgress(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        CircularProgressIndicator()
    }
}

/**
 * One centred state: a symbol over a message and, for a failure, a retry action.
 * Used for the prompt, no-match and failure states so the screen never shows
 * bare top-left text.
 */
@Composable
private fun ManageUsersStateMessage(
    icon: String,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(24.dp),
    ) {
        MaterialSymbol(
            glyph = icon,
            contentDescription = null,
            size = 48.sp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.let {
            Spacer(modifier = Modifier.height(8.dp))
            it()
        }
    }
}

/** The rounded search field pinned above the results, matching the other screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageUsersSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
) {
    val focusManager = LocalFocusManager.current

    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        leadingIcon = { MaterialSymbol(glyph = "search", contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    MaterialSymbol(glyph = "close", contentDescription = clearDescription)
                }
            }
        },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(MANAGE_USERS_SEARCH_TAG),
    )
}
