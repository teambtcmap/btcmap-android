package org.btcmap.auth

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.CreateTokenResponse
import org.btcmap.api.createUser
import org.btcmap.api.signIn
import org.btcmap.api.toDbUser
import org.btcmap.db
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.settings.prefs
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.userFacingMessage
import kotlin.coroutines.coroutineContext

/**
 * Shows the account chooser and, once the user picks an option, the sign-in or
 * sign-up form.
 *
 * The submitted credentials are delivered to [registerAuthResultListener], not
 * through a callback held by a dialog fragment, so a form that is still open
 * when the device is rotated can still be submitted. [extras] is passed through
 * unchanged and returned to the listener, so the caller can resume the action
 * that needed an account.
 */
fun Fragment.showAuthDialog(extras: Bundle? = null) {
    if (!isAdded) return

    AuthChooserDialogFragment.newInstance(extras)
        .show(childFragmentManager, AuthChooserDialogFragment.TAG)
}

/**
 * Registers the receiver of submitted sign-in/sign-up forms.
 *
 * Call this from `onViewCreated` so a fragment recreated after a configuration
 * change also receives a form submitted afterwards. [onAuthenticated] runs only
 * once the session has been stored, with the [extras] given to [showAuthDialog].
 */
fun Fragment.registerAuthResultListener(onAuthenticated: (extras: Bundle) -> Unit) {
    childFragmentManager.setFragmentResultListener(
        AuthDialogFragment.REQUEST_KEY,
        viewLifecycleOwner,
    ) { _, result ->
        val modeName = result.getString(AuthDialogFragment.MODE)
            ?: return@setFragmentResultListener
        val mode = try {
            AuthMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            null
        } ?: return@setFragmentResultListener
        val username = result.getString(AuthDialogFragment.USERNAME).orEmpty()
        val password = result.getString(AuthDialogFragment.PASSWORD).orEmpty()
        val extras = result.getBundle(AuthDialogFragment.EXTRAS) ?: Bundle()

        when (mode) {
            AuthMode.SignIn -> signIn(username, password, extras, onAuthenticated)
            AuthMode.SignUp -> signUp(username, password, extras, onAuthenticated)
        }
    }
}

/**
 * Shows the change-password form. The submitted passwords are delivered to
 * [registerChangePasswordResultListener].
 */
fun Fragment.showChangePasswordDialog() {
    if (!isAdded) return

    ChangePasswordDialogFragment.newInstance()
        .show(childFragmentManager, ChangePasswordDialogFragment.TAG)
}

/**
 * Registers the receiver of a submitted change-password form. Call this from
 * `onViewCreated` so the listener is re-established after a configuration
 * change. [onSubmit] receives the entered current and new password.
 */
fun Fragment.registerChangePasswordResultListener(
    onSubmit: (current: String, new: String) -> Unit,
) {
    childFragmentManager.setFragmentResultListener(
        ChangePasswordDialogFragment.REQUEST_KEY,
        viewLifecycleOwner,
    ) { _, result ->
        val current = result.getString(ChangePasswordDialogFragment.CURRENT_PASSWORD).orEmpty()
        val new = result.getString(ChangePasswordDialogFragment.NEW_PASSWORD).orEmpty()
        onSubmit(current, new)
    }
}

private fun Fragment.signUp(
    username: String,
    password: String,
    extras: Bundle,
    onAuthenticated: (extras: Bundle) -> Unit,
) {
    viewLifecycleOwner.lifecycleScope.launch {
        val user = runAuthRequest(
            logMessage = "Failed to create new account",
            fallbackMessage = getString(R.string.failed_to_create_new_account),
        ) {
            api().createUser(name = username, password = password)
        } ?: return@launch

        // The account exists now. If signing in fails, fall back to the sign-in
        // form instead of surfacing a second error dialog: the caller is told
        // what happened with a toast.
        val response = runAuthRequest(
            logMessage = "Failed to sign in after creating a new account",
            fallbackMessage = getString(R.string.failed_to_sign_in),
            showErrorDialog = false,
        ) {
            api().signIn(
                username = user.name,
                password = password,
                label = tokenLabel(),
            )
        }

        if (response == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.account_created_sign_in_failed),
                Toast.LENGTH_LONG,
            ).show()
            if (isAdded) {
                AuthDialogFragment.newCredentials(
                    mode = AuthMode.SignIn,
                    extras = extras,
                    prefilledUsername = user.name,
                ).show(childFragmentManager, AuthDialogFragment.TAG)
            }
            return@launch
        }

        completeSignIn(response, extras, onAuthenticated)
    }
}

private fun Fragment.signIn(
    username: String,
    password: String,
    extras: Bundle,
    onAuthenticated: (extras: Bundle) -> Unit,
) {
    viewLifecycleOwner.lifecycleScope.launch {
        val response = runAuthRequest(
            logMessage = "Sign in failed",
            fallbackMessage = getString(R.string.failed_to_sign_in),
        ) {
            api().signIn(
                username = username,
                password = password,
                label = tokenLabel(),
            )
        } ?: return@launch

        completeSignIn(response, extras, onAuthenticated)
    }
}

/**
 * Runs [request] behind a cancelable progress dialog, with a timeout so a slow
 * server cannot trap the user behind a spinner. Returns null on failure, after
 * reporting it (with a dialog when [showErrorDialog] is true, otherwise only in
 * the log).
 */
private suspend fun <T> Fragment.runAuthRequest(
    logMessage: String,
    fallbackMessage: String,
    showErrorDialog: Boolean = true,
    request: suspend () -> T,
): T? {
    val context = coroutineContext
    val progress = showProgressDialog(R.string.loading) { context.cancel() }

    try {
        return withTimeout(AUTH_TIMEOUT_MS) { request() }
    } catch (e: TimeoutCancellationException) {
        reportAuthError(logMessage, e, fallbackMessage, showErrorDialog)
        return null
    } catch (e: Exception) {
        e.rethrowIfCancellation()
        reportAuthError(logMessage, e, fallbackMessage, showErrorDialog)
        return null
    } finally {
        progress.dismiss()
    }
}

private fun Fragment.reportAuthError(
    logMessage: String,
    e: Throwable,
    fallbackMessage: String,
    showDialog: Boolean,
) {
    if (showDialog) {
        showAuthError(logMessage, e, fallbackMessage)
    } else {
        Log.e(AUTH_TAG, logMessage, e)
    }
}

private suspend fun Fragment.completeSignIn(
    response: CreateTokenResponse,
    extras: Bundle,
    onAuthenticated: (extras: Bundle) -> Unit,
) {
    try {
        storeSignedInSession(db = db(), prefs = prefs, response = response)
    } catch (e: Exception) {
        e.rethrowIfCancellation()
        showAuthError(
            logMessage = "Failed to store signed-in account",
            e = e,
            fallbackMessage = getString(R.string.failed_to_sign_in),
        )
        return
    }

    // The session is durable now. If the view was destroyed in the meantime the
    // coroutine is cancelled before the callback runs, so [onAuthenticated] may
    // be skipped; the account is still signed in and the next screen sees it.
    val toastContext = context
    if (toastContext != null) {
        Toast.makeText(
            toastContext,
            getString(R.string.logged_in_as, response.user.name),
            Toast.LENGTH_SHORT,
        ).show()
    }

    try {
        onAuthenticated(extras)
    } catch (e: Exception) {
        e.rethrowIfCancellation()
        Log.e(AUTH_TAG, "Signed-in callback failed", e)
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

private fun Fragment.showProgressDialog(
    @StringRes message: Int,
    onCancel: () -> Unit,
): AlertDialog {
    val dialogView = layoutInflater.inflate(R.layout.account_progress_dialog, null)
    dialogView.findViewById<TextView>(R.id.progressMessage).setText(message)

    return MaterialAlertDialogBuilder(requireContext())
        .setView(dialogView)
        .setCancelable(true)
        .setOnCancelListener { onCancel() }
        .create()
        .also {
            it.show()
            dismissOnViewDestroyed(it)
        }
}

private fun Fragment.showAuthError(logMessage: String, e: Throwable, fallbackMessage: String) {
    Log.e(AUTH_TAG, logMessage, e)

    if (!isAdded) return

    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.error)
        .setMessage(e.userFacingMessage(fallbackMessage))
        .setPositiveButton(android.R.string.ok, null)
        .create()

    dialog.show()
    dismissOnViewDestroyed(dialog)
}

/**
 * Dismisses [dialog] when the fragment's view is destroyed so a dialog that is
 * still showing during a configuration change does not leak the activity window.
 */
private fun Fragment.dismissOnViewDestroyed(dialog: AlertDialog) {
    val owner = viewLifecycleOwner
    val observer = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dialog.dismiss()
        }
    }
    owner.lifecycle.addObserver(observer)
    dialog.setOnDismissListener {
        owner.lifecycle.removeObserver(observer)
    }
}

private fun tokenLabel(): String = buildString {
    append("BTC Map Android ")
    append(BuildConfig.VERSION_CODE)
    val device = listOf(Build.MANUFACTURER, Build.MODEL)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    if (device.isNotEmpty()) {
        append(' ')
        append(device)
    }
}

private const val AUTH_TIMEOUT_MS = 30_000L

private const val AUTH_TAG = "auth"
