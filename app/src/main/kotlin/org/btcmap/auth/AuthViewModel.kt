package org.btcmap.auth

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.btcmap.BuildConfig
import org.btcmap.api.Api
import org.btcmap.api.CreateTokenResponse
import org.btcmap.api.createUser
import org.btcmap.api.signIn
import org.btcmap.api.toDbUser
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.util.rethrowIfCancellation

/** Which credential flow produced a failure, so the view can pick its message. */
internal enum class AuthOperation {
    SignIn,
    SignUp,
}

/** One-shot outcomes of an authentication attempt, delivered exactly once. */
internal sealed interface AuthEvent {
    /** Signed in and the session is stored; [extras] is the caller's payload. */
    data class Authenticated(val name: String, val extras: Bundle) : AuthEvent

    /** The account was created but the follow-up sign-in failed. */
    data class AccountCreated(val username: String, val extras: Bundle) : AuthEvent

    /** The request failed; the view shows [error] with [operation]'s fallback. */
    data class Failed(val error: Throwable, val operation: AuthOperation) : AuthEvent
}

/**
 * Runs the sign-in and sign-up requests and stores the resulting session.
 *
 * The requests run in the [viewModelScope], not the view's, so a configuration
 * change mid-request does not cancel them. Outcomes are delivered through
 * [events] and buffered while no view is collecting them, so a form submitted
 * just before a rotation still reaches the recreated screen.
 */
internal class AuthViewModel(
    private val api: Api,
    private val db: Database,
    private val prefs: Settings,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)

    /** True while a request is in flight; the view shows a progress dialog. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    // Unlimited so a one-shot outcome is never dropped: the events are tiny and
    // produced at most once per request, and an outcome silently lost while no
    // view is attached would strand the action that asked for the account.
    private val _events = Channel<AuthEvent>(Channel.UNLIMITED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    private var job: Job? = null

    fun signIn(username: String, password: String, extras: Bundle) {
        start { performSignIn(username, password, extras) }
    }

    fun signUp(username: String, password: String, extras: Bundle) {
        start { performSignUp(username, password, extras) }
    }

    /** Cancels the in-flight request, e.g. when the progress dialog is dismissed. */
    fun cancel() {
        job?.cancel()
    }

    private fun start(block: suspend () -> Unit) {
        if (job?.isActive == true) return

        job = viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun performSignIn(username: String, password: String, extras: Bundle) {
        val response = request(AuthOperation.SignIn) {
            api.signIn(username = username, password = password, label = tokenLabel())
        } ?: return

        completeSignIn(response, extras)
    }

    private suspend fun performSignUp(username: String, password: String, extras: Bundle) {
        val user = request(AuthOperation.SignUp) {
            api.createUser(name = username, password = password)
        } ?: return

        // The account exists now. If signing in fails, the view falls back to
        // the sign-in form instead of surfacing a second error dialog.
        val response = request(AuthOperation.SignUp, showError = false) {
            api.signIn(username = user.name, password = password, label = tokenLabel())
        }

        if (response == null) {
            _events.trySend(AuthEvent.AccountCreated(username = user.name, extras = extras))
            return
        }

        completeSignIn(response, extras)
    }

    /**
     * Runs [request] behind a timeout. Returns null and queues a
     * [AuthEvent.Failed] on failure, unless [showError] is false (used when the
     * caller reports the failure itself).
     */
    private suspend fun <T> request(
        operation: AuthOperation,
        showError: Boolean = true,
        request: suspend () -> T,
    ): T? {
        return try {
            withTimeout(AUTH_TIMEOUT_MS) { request() }
        } catch (e: TimeoutCancellationException) {
            if (showError) _events.trySend(AuthEvent.Failed(e, operation))
            null
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            if (showError) _events.trySend(AuthEvent.Failed(e, operation))
            null
        }
    }

    private suspend fun completeSignIn(response: CreateTokenResponse, extras: Bundle) {
        try {
            storeSignedInSession(db = db, prefs = prefs, response = response)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            _events.trySend(AuthEvent.Failed(e, AuthOperation.SignIn))
            return
        }

        _events.trySend(AuthEvent.Authenticated(name = response.user.name, extras = extras))
    }

    private fun tokenLabel(): String = buildString {
        append("BTC Map Android ")
        append(BuildConfig.VERSION_CODE)
        // Build.MANUFACTURER/MODEL are platform strings that can be null on a
        // few devices (and in a JVM test), so trim defensively.
        val device = listOf(Build.MANUFACTURER, Build.MODEL)
            .mapNotNull { it?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (device.isNotEmpty()) {
            append(' ')
            append(device)
        }
    }

    class Factory(
        private val api: Api,
        private val db: Database,
        private val prefs: Settings,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(api, db, prefs) as T
        }
    }

    companion object {
        private const val AUTH_TIMEOUT_MS = 30_000L
    }
}

/**
 * Persists a successful sign-in. The token and the cached user are written in a
 * single database transaction, so a failure can never leave the account only
 * partly stored and the previous session is kept intact.
 */
internal suspend fun storeSignedInSession(
    db: Database,
    prefs: Settings,
    response: CreateTokenResponse,
) {
    withContext(Dispatchers.IO) {
        prefs.replaceSession(
            db = db,
            token = response.token,
            user = response.user.toDbUser(),
        )
    }
}
