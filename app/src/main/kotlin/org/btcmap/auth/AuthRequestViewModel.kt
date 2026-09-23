package org.btcmap.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Shared plumbing for the one-shot account requests (sign-in, sign-up and
 * change-password): a [busy] flag for the progress dialog, an [events] channel
 * for the exactly-once outcome, and a single cancellable job bounded by
 * [withRequestTimeout].
 *
 * The request runs in the [viewModelScope], not the view's, so a configuration
 * change mid-request does not cancel it. Outcomes are delivered through
 * [events] and buffered while no view is collecting them, so a form submitted
 * just before a rotation still reaches the recreated screen.
 */
internal abstract class AuthRequestViewModel<E : Any> : ViewModel() {

    private val _busy = MutableStateFlow(false)

    /** True while a request is in flight; the view shows a progress dialog. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    // Unlimited so a one-shot outcome is never dropped: the events are tiny and
    // produced at most once per request, and an outcome silently lost while no
    // view is attached would strand the action that asked for the account.
    private val _events = Channel<E>(Channel.UNLIMITED)
    val events: Flow<E> = _events.receiveAsFlow()

    private var job: Job? = null

    /**
     * Identifies the most recent request. A cancelled request may still be
     * unwinding when the request that replaced it has already started, so its
     * `finally` must not clear the busy flag of that newer request.
     */
    private var requestId = 0L

    /** Runs [block] unless a request is already in flight, which it ignores. */
    protected fun launchRequest(block: suspend () -> Unit) {
        if (job?.isActive == true) return

        val id = ++requestId
        _busy.value = true
        job = viewModelScope.launch {
            try {
                block()
            } finally {
                // Only the latest request may clear the flag: a cancelled request
                // that raced a newer one must not report the newer one as idle.
                if (id == requestId) _busy.value = false
            }
        }
    }

    /** Cancels the in-flight request, e.g. when the progress dialog is dismissed. */
    fun cancel() {
        job?.cancel()
    }

    /** Queues an outcome for the view. */
    protected fun emit(event: E) {
        _events.trySend(event)
    }

    /** Bounds [block] by the shared request timeout. */
    protected suspend fun <T> withRequestTimeout(block: suspend () -> T): T =
        withTimeout(REQUEST_TIMEOUT_MS) { block() }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 30_000L
    }
}
