package org.btcmap.auth

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.btcmap.BuildConfig
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.btcmap.settings.Settings

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
 * The requests themselves live in the shared [AuthSession], so Android and the
 * desktop share the sign-in and the sign-up ambiguity handling. This adds the
 * retained, timed, exactly-once delivery inherited from
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
        val outcome = AuthSession.signIn(api, db, prefs, username, password, tokenLabel())
        when (outcome) {
            is AuthOutcome.Authenticated -> emit(AuthEvent.Authenticated(outcome.name, extras))
            is AuthOutcome.AccountCreated -> emit(AuthEvent.AccountCreated(outcome.username, extras))
            is AuthOutcome.Failed -> emit(AuthEvent.Failed(outcome.error, AuthOperation.SignIn))
        }
    }

    private suspend fun performSignUp(username: String, password: String, extras: Bundle) {
        val outcome = AuthSession.signUp(
            api = api,
            db = db,
            prefs = prefs,
            username = username,
            password = password,
            label = tokenLabel(),
            // An account created just before the user cancelled is reported here
            // rather than through the return value, which the cancelled
            // coroutine never reaches.
            onAccountCreated = { emit(AuthEvent.AccountCreated(it, extras)) },
        )
        when (outcome) {
            is AuthOutcome.Authenticated -> emit(AuthEvent.Authenticated(outcome.name, extras))
            is AuthOutcome.AccountCreated -> emit(AuthEvent.AccountCreated(outcome.username, extras))
            is AuthOutcome.Failed -> emit(AuthEvent.Failed(outcome.error, AuthOperation.SignUp))
        }
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
