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
 * A guarded request's outcome, so a caller can inspect a failure instead of
 * having it queued as an event immediately. Coroutine cancellation is never
 * represented here; it propagates from the request runner untouched.
 */
private sealed interface RequestOutcome<out T> {
    data class Success<T>(val value: T) : RequestOutcome<T>

    data class Failure(val error: Throwable) : RequestOutcome<Nothing>
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
        val response = when (val outcome = runRequest {
            api.signIn(username = username, password = password, label = tokenLabel())
        }) {
            is RequestOutcome.Failure -> {
                emit(AuthEvent.Failed(outcome.error, AuthOperation.SignIn))
                return
            }

            is RequestOutcome.Success -> outcome.value
        }

        val failure = storeSession(response)
        if (failure != null) {
            emit(AuthEvent.Failed(failure, AuthOperation.SignIn))
            return
        }

        emit(AuthEvent.Authenticated(name = response.user.name, extras = extras))
    }

    private suspend fun performSignUp(username: String, password: String, extras: Bundle) {
        val creation = runRequest {
            api.createUser(name = username, password = password)
        }

        // A failed creation is ambiguous: the server may have created the account
        // before the response was lost (a transport failure, a timeout or an
        // error after the row was committed), so signing in with the same
        // credentials tells the two apart. Success means the account exists; a
        // failure settles it, and the original creation error is the one to
        // report. The name the server assigned is unknown when creation failed,
        // so fall back to the one the user typed.
        val name = when (creation) {
            is RequestOutcome.Success -> creation.value.name
            is RequestOutcome.Failure -> username
        }

        // The follow-up sign-in is not reported on its own: success signs the
        // user in, while a failure means either that the account was created but
        // could not be signed in (the view then falls back to the sign-in form)
        // or that the creation itself failed. The same applies when the user
        // cancels the sign-in: an account that was created must not be silently
        // swallowed, or a retried sign-up would fail as already taken.
        val response = try {
            requestWithoutReporting {
                api.signIn(username = name, password = password, label = tokenLabel())
            }
        } catch (e: CancellationException) {
            if (creation is RequestOutcome.Success) {
                emit(AuthEvent.AccountCreated(username = name, extras = extras))
            }
            throw e
        }

        if (response == null) {
            when (creation) {
                is RequestOutcome.Success ->
                    emit(AuthEvent.AccountCreated(username = name, extras = extras))

                is RequestOutcome.Failure ->
                    emit(AuthEvent.Failed(creation.error, AuthOperation.SignUp))
            }
            return
        }

        // A session that cannot be stored locally is reported like a failed
        // automatic sign-in: the user is told the account was created and the
        // view falls back to the sign-in form, rather than claiming the account
        // could not be created and sending a retry into "username already taken".
        val failure = storeSession(response)
        if (failure != null) {
            emit(AuthEvent.AccountCreated(username = name, extras = extras))
            return
        }

        emit(AuthEvent.Authenticated(name = response.user.name, extras = extras))
    }

    /** Runs [request] and returns its value, or null when it failed. */
    private suspend fun <T> requestWithoutReporting(request: suspend () -> T): T? =
        when (val outcome = runRequest(request)) {
            is RequestOutcome.Success -> outcome.value
            is RequestOutcome.Failure -> null
        }

    /**
     * Runs [request] behind the shared request timeout, turning a failure into
     * an outcome the caller can inspect. A timeout becomes a
     * [RequestOutcome.Failure] because the caller must decide what a stalled
     * request means; any other coroutine cancellation is not a failure and
     * propagates untouched.
     */
    private suspend fun <T> runRequest(request: suspend () -> T): RequestOutcome<T> {
        return try {
            RequestOutcome.Success(withRequestTimeout { request() })
        } catch (e: TimeoutCancellationException) {
            RequestOutcome.Failure(e)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            RequestOutcome.Failure(e)
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
