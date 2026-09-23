package org.btcmap.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.TimeoutCancellationException
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
 * Runs the change-password request. Inherits the retained, timed, exactly-once
 * request machinery from [AuthRequestViewModel], so a configuration change
 * mid-request does not cancel it and a failure reaches the recreated screen
 * instead of being lost with the previous view.
 */
internal class ChangePasswordViewModel(
    private val api: Api,
) : AuthRequestViewModel<ChangePasswordEvent>() {

    /** Sends the new password, ignoring a call while one is already in flight. */
    fun change(currentPassword: String, newPassword: String) {
        launchRequest {
            try {
                withRequestTimeout {
                    api.updatePassword(oldPassword = currentPassword, newPassword = newPassword)
                }
                emit(ChangePasswordEvent.Changed)
            } catch (e: TimeoutCancellationException) {
                emit(ChangePasswordEvent.Failed(e))
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                emit(ChangePasswordEvent.Failed(e))
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
