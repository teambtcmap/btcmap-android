package org.btcmap.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.btcmap.api.Api
import org.btcmap.api.CreateTokenResponse
import org.btcmap.api.createUser
import org.btcmap.api.signIn
import org.btcmap.api.toDbUser
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.util.rethrowIfCancellation

/** The outcome of a sign-in or sign-up attempt. */
sealed interface AuthOutcome {
    /** Signed in and the session was stored; [name] is the server's name. */
    data class Authenticated(val name: String) : AuthOutcome

    /**
     * The account was created but the session could not be established: the
     * automatic sign-in failed or timed out, or its session could not be
     * stored. The caller falls back to the sign-in form.
     */
    data class AccountCreated(val username: String) : AuthOutcome

    /** The request failed before a session existed; [error] is what to report. */
    data class Failed(val error: Throwable) : AuthOutcome
}

/**
 * Establishes a sign-in session the way every host needs it, so Android and the
 * desktop share one implementation of the sign-in request and of the sign-up
 * ambiguity handling.
 *
 * Each network step is bounded by [REQUEST_TIMEOUT_MS], and a timeout is
 * reported as a failure rather than a cancellation. Genuine coroutine
 * cancellation is never swallowed: it propagates, but [signUp]'s
 * [onAccountCreated] callback lets a caller report an account that was created
 * before the cancellation, which the returned [AuthOutcome] cannot carry across
 * a throw.
 */
object AuthSession {

    /**
     * Signs in and stores the resulting session. A failure to store a session
     * the server issued is reported as a sign-in failure.
     */
    suspend fun signIn(
        api: Api,
        db: Database,
        prefs: Settings,
        username: String,
        password: String,
        label: String,
    ): AuthOutcome {
        val response = when (val outcome = runRequest {
            api.signIn(username = username, password = password, label = label)
        }) {
            is RequestOutcome.Failure -> return AuthOutcome.Failed(outcome.error)
            is RequestOutcome.Success -> outcome.value
        }

        val failure = storeSession(db, prefs, response)
        return if (failure != null) {
            AuthOutcome.Failed(failure)
        } else {
            AuthOutcome.Authenticated(response.user.name)
        }
    }

    /**
     * Creates the account and then signs in with it.
     *
     * A failed creation is ambiguous: the server may have created the account
     * before the response was lost (a transport failure, a timeout or an error
     * after the row was committed), so signing in with the same credentials
     * tells the two apart. Success means the account exists; a failure settles
     * it, and then the original creation error is the one to report. The name
     * the server assigned is unknown when creation failed, so fall back to the
     * one the user typed.
     *
     * The follow-up sign-in is not reported on its own: success signs the user
     * in, while a failure means either that the account was created but could
     * not be signed in (the caller then falls back to the sign-in form) or that
     * the creation itself failed. The same applies when the user cancels the
     * sign-in: an account that was created must not be silently swallowed, or a
     * retried sign-up would fail as already taken, so [onAccountCreated] is
     * invoked before the cancellation propagates.
     */
    suspend fun signUp(
        api: Api,
        db: Database,
        prefs: Settings,
        username: String,
        password: String,
        label: String,
        onAccountCreated: (String) -> Unit = {},
    ): AuthOutcome {
        val creation = runRequest { api.createUser(name = username, password = password) }

        val name = when (creation) {
            is RequestOutcome.Success -> creation.value.name
            is RequestOutcome.Failure -> username
        }

        val response = try {
            requestWithoutReporting { api.signIn(username = name, password = password, label = label) }
        } catch (e: CancellationException) {
            if (creation is RequestOutcome.Success) onAccountCreated(name)
            throw e
        }

        if (response == null) {
            return when (creation) {
                is RequestOutcome.Success -> AuthOutcome.AccountCreated(name)
                is RequestOutcome.Failure -> AuthOutcome.Failed(creation.error)
            }
        }

        // A session that cannot be stored locally is reported like a failed
        // automatic sign-in: the user is told the account was created and the
        // caller falls back to the sign-in form, rather than claiming the
        // account could not be created and sending a retry into "username
        // already taken".
        val failure = storeSession(db, prefs, response)
        return if (failure != null) {
            AuthOutcome.AccountCreated(name)
        } else {
            AuthOutcome.Authenticated(response.user.name)
        }
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
    private suspend fun <T> runRequest(request: suspend () -> T): RequestOutcome<T> =
        try {
            RequestOutcome.Success(withTimeout(REQUEST_TIMEOUT_MS) { request() })
        } catch (e: TimeoutCancellationException) {
            RequestOutcome.Failure(e)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            RequestOutcome.Failure(e)
        }

    /**
     * Persists the signed-in session, returning the failure that prevented it,
     * or null on success. The caller decides how to report a failure: a sign-in
     * that could not be stored is a sign-in failure, while a sign-up that could
     * not be stored still created the account.
     */
    private suspend fun storeSession(
        db: Database,
        prefs: Settings,
        response: CreateTokenResponse,
    ): Throwable? =
        try {
            storeSignedInSession(db = db, prefs = prefs, response = response)
            null
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            e
        }

    private const val REQUEST_TIMEOUT_MS = 30_000L
}

private sealed interface RequestOutcome<out T> {
    data class Success<T>(val value: T) : RequestOutcome<T>

    data class Failure(val error: Throwable) : RequestOutcome<Nothing>
}

/**
 * Persists a successful sign-in. The token and the cached user are written in a
 * single database transaction, so a failure can never leave the account only
 * partly stored and the previous session is kept intact.
 */
suspend fun storeSignedInSession(
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
 * [manufacturer] and [model] come from `Build` on Android and can each be null
 * on a few devices (and in a JVM test), so they are optional and trimmed before
 * use; a missing or blank value is dropped instead of leaving a stray space.
 */
fun authTokenLabel(manufacturer: String?, model: String?, versionCode: Int): String =
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
