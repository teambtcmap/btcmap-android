package org.btcmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.withContext
import org.btcmap.account.canManageEvents
import org.btcmap.api.Api
import org.btcmap.api.getMyEvents
import org.btcmap.db.Database
import org.btcmap.platform.ioDispatcher
import org.btcmap.util.rethrowIfCancellation

/** Test tags for the event delete action. */
const val EVENT_DELETE_TAG = "event-delete"
const val EVENT_DELETE_CONFIRM_TAG = "event-delete-confirm"
const val EVENT_DELETE_ERROR_TAG = "event-delete-error"

/**
 * Whether the signed-in user may delete an event, and how: through the
 * privileged `delete_event` RPC (any event), or their own pending-submission
 * revoke.
 */
data class EventDeleteState(
    val canDelete: Boolean = false,
    val withRpc: Boolean = false,
)

/**
 * Resolves whether the signed-in user may delete [eventId]: a privileged role
 * may delete any event, and everyone else may revoke a pending event they
 * submitted. The client only knows authorship from the list of the user's own
 * events, so that is fetched for a non-privileged user (and, offline, the delete
 * is simply not offered). The server stays authoritative.
 */
@Composable
fun rememberEventDeleteState(
    db: Database,
    api: Api,
    eventId: Long,
): EventDeleteState {
    var state by remember(eventId) { mutableStateOf(EventDeleteState()) }

    LaunchedEffect(eventId) {
        val user = withContext(ioDispatcher) { db.user.select() }
        if (user == null) {
            state = EventDeleteState()
            return@LaunchedEffect
        }

        val privileged = user.canManageEvents()
        val mine = if (privileged) {
            null
        } else {
            try {
                api.getMyEvents().firstOrNull { it.id == eventId }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                null
            }
        }

        state = EventDeleteState(
            canDelete = privileged || mine?.status == "pending",
            withRpc = privileged,
        )
    }

    return state
}

/**
 * The event delete action for a screen's top bar: a delete icon that opens a
 * confirmation and then runs [onDelete]. When [onDelete] is null — the signed-in
 * user may not delete this event — no action is drawn at all.
 *
 * [onDeleted] runs after a successful delete so the host can leave the screen. A
 * failure keeps the confirmation open with [EventScreenLabels.deleteFailed]
 * shown, so a retry is one tap away.
 */
@Composable
fun EventDeleteAction(
    labels: EventScreenLabels,
    onDelete: (suspend () -> Unit)?,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier,
        deleteTag = EVENT_DELETE_TAG,
        confirmTag = EVENT_DELETE_CONFIRM_TAG,
        errorTag = EVENT_DELETE_ERROR_TAG,
    )
}
