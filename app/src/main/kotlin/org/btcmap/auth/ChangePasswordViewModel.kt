package org.btcmap.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.btcmap.api.Api
import org.btcmap.api.updatePassword
import org.btcmap.util.rethrowIfCancellation

/** One-shot outcomes of a change-password request, delivered exactly once. */
internal sealed interface ChangePasswordEvent {
    /** The server accepted the new password. */
    data object Changed : ChangePasswordEvent

    /** The request failed; the view shows [error]. */
    data class Failed(val error: Throwable) : ChangePasswordEvent
}

/**
 * Runs the change-password request in the [viewModelScope], not the view's, so a
 * configuration change mid-request does not cancel it. Outcomes are delivered
 * through [events] and buffered while no view is collecting them, so a failure
 * reaches the recreated screen instead of being lost with the previous view.
 */
internal class ChangePasswordViewModel(
    private val api: Api,
) : ViewModel() {

    // Unlimited so a one-shot outcome is never dropped: the events are tiny and
    // produced at most once per request, and an outcome silently lost while no
    // view is attached would leave the change with no feedback.
    private val _events = Channel<ChangePasswordEvent>(Channel.UNLIMITED)
    val events: Flow<ChangePasswordEvent> = _events.receiveAsFlow()

    private var job: Job? = null

    fun change(currentPassword: String, newPassword: String) {
        if (job?.isActive == true) return

        job = viewModelScope.launch {
            try {
                api.updatePassword(oldPassword = currentPassword, newPassword = newPassword)
                _events.trySend(ChangePasswordEvent.Changed)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                _events.trySend(ChangePasswordEvent.Failed(e))
            }
        }
    }

    class Factory(
        private val api: Api,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChangePasswordViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return ChangePasswordViewModel(api) as T
        }
    }
}
