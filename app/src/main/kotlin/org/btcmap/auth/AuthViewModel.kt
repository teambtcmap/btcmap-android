package org.btcmap.auth

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
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

    /**
     * The account was created but the session could not be established: the
     * automatic sign-in failed or timed out, or its session could not be
     * stored. The view falls back to the sign-in form.
     */
    data class AccountCreated(val username: String, val extras: Bundle) : AuthEvent

    /** The request failed; the view shows [error] with [operation]'s fallback. */
    data class Failed(val error: Throwable, val operation: AuthOperation) : AuthEvent
}

/**
 * Runs the sign-in and sign-up requests and stores the resulting session.
 *
 * Inherits the retained, timed, exactly-once request machinery from
 * [AuthRequestViewModel], so a request survives a configuration change and a
 * form submitted just before a rotation still reaches the recreated screen.
 */
internal class AuthViewModel(
    private val api: Api,
    private val db: Database,
    private val prefs: Settings,
) : AuthRequestViewModel<AuthEvent>() {

    fun signIn(username: String, password: String, extras: Bundle) {
        launchRequest { performSignIn(username, password, extras) }
    }

    fun signUp(username: String, password: String, extras: Bundle) {
        launchRequest { performSignUp(username, password, extras) }
    }

    private suspend fun performSignIn(username: String, password: String, extras: Bundle) {
        val response = request(AuthOperation.SignIn) {
            api.signIn(username = username, password = password, label = tokenLabel())
        } ?: return

        val failure = storeSession(response)
        if (failure != null) {
            emit(AuthEvent.Failed(failure, AuthOperation.SignIn))
            return
        }

        emit(AuthEvent.Authenticated(name = response.user.name, extras = extras))
    }

    private suspend fun performSignUp(username: String, password: String, extras: Bundle) {
        val user = request(AuthOperation.SignUp) {
            api.createUser(name = username, password = password)
        } ?: return

        // The account exists now, so a failure of the follow-up sign-in is
        // reported as an [AuthEvent.AccountCreated] (the view then falls back to
        // the sign-in form) instead of surfacing a second error dialog. The same
        // applies when the user cancels the request: the account must not be
        // silently swallowed, or a retried sign-up would fail as already taken.
        val response = try {
            request(AuthOperation.SignUp, showError = false) {
                api.signIn(username = user.name, password = password, label = tokenLabel())
            }
        } catch (e: CancellationException) {
            emit(AuthEvent.AccountCreated(username = user.name, extras = extras))
            throw e
        }

        if (response == null) {
            emit(AuthEvent.AccountCreated(username = user.name, extras = extras))
            return
        }

        // The account exists, so a session that cannot be stored locally is
        // reported the same way as a failed automatic sign-in: the user is told
        // the account was created and the view falls back to the sign-in form,
        // rather than claiming the account could not be created and sending a
        // retry into "username already taken".
        val failure = storeSession(response)
        if (failure != null) {
            emit(AuthEvent.AccountCreated(username = user.name, extras = extras))
            return
        }

        emit(AuthEvent.Authenticated(name = response.user.name, extras = extras))
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
            withRequestTimeout { request() }
        } catch (e: TimeoutCancellationException) {
            if (showError) emit(AuthEvent.Failed(e, operation))
            null
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            if (showError) emit(AuthEvent.Failed(e, operation))
            null
        }
    }

    /**
     * Persists the signed-in session, returning the failure that prevented it,
     * or null on success. The caller decides how to report a failure: a sign-in
     * that could not be stored is a sign-in failure, while a sign-up that could
     * not be stored still created the account.
     */
    private suspend fun storeSession(response: CreateTokenResponse): Throwable? =
        try {
            storeSignedInSession(db = db, prefs = prefs, response = response)
            null
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            e
        }

    private fun tokenLabel(): String = authTokenLabel(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        versionCode = BuildConfig.VERSION_CODE,
    )

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

/**
 * A label for a token this app creates, shown in the account's token list on
 * btcmap.org.
 *
 * [manufacturer] and [model] come from `Build` and can each be null on a few
 * devices (and in a JVM test), so they are optional and trimmed before use; a
 * missing or blank value is dropped instead of leaving a stray space.
 */
internal fun authTokenLabel(manufacturer: String?, model: String?, versionCode: Int): String =
    buildString {
        append("BTC Map Android ")
        append(versionCode)
        val device = listOf(manufacturer, model)
            .mapNotNull { it?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (device.isNotEmpty()) {
            append(' ')
            append(device)
        }
    }
