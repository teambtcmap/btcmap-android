package org.btcmap.auth

import android.content.DialogInterface
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

fun Fragment.showAuthDialog(onSuccess: () -> Unit) {
    if (!isAdded) return

    val dialogView = layoutInflater.inflate(R.layout.account_choices_dialog, null)
    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.account)
        .setView(dialogView)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialogView.findViewById<View>(R.id.createAccountOption).setOnClickListener {
        dialog.dismiss()
        createNewAccount(onSuccess)
    }
    dialogView.findViewById<View>(R.id.signInOption).setOnClickListener {
        dialog.dismiss()
        showSignInDialog(onSuccess)
    }

    dialog.show()
    dismissOnViewDestroyed(dialog)
}

private fun Fragment.createNewAccount(onComplete: () -> Unit) {
    showCredentialsDialog(
        title = R.string.new_account,
        positiveButton = R.string.sign_up,
    ) { username, password ->
        signUp(username, password, onComplete)
    }
}

private fun Fragment.showSignInDialog(
    onComplete: () -> Unit,
    prefilledUsername: String? = null,
) {
    showCredentialsDialog(
        title = R.string.login,
        positiveButton = R.string.login,
        prefilledUsername = prefilledUsername,
    ) { username, password ->
        signIn(username, password, onComplete)
    }
}

private fun Fragment.showCredentialsDialog(
    @StringRes title: Int,
    @StringRes positiveButton: Int,
    prefilledUsername: String? = null,
    onSubmit: (username: String, password: String) -> Unit,
) {
    if (!isAdded) return

    val dialogView = layoutInflater.inflate(R.layout.account_dialog, null)
    val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
    val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.passwordInput)

    prefilledUsername?.let { usernameInput.setText(it) }

    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(title)
        .setView(dialogView)
        .setPositiveButton(positiveButton, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()

            var valid = true
            if (username.isEmpty()) {
                usernameInput.error = getString(R.string.field_required)
                valid = false
            }
            if (password.isEmpty()) {
                passwordInput.error = getString(R.string.field_required)
                valid = false
            }
            if (!valid) return@setOnClickListener

            dialog.dismiss()
            onSubmit(username, password)
        }
    }

    dialog.show()
    dismissOnViewDestroyed(dialog)
}

private fun Fragment.signUp(username: String, password: String, onComplete: () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        val progress = showProgressDialog(R.string.loading)

        try {
            val user = try {
                api().createUser(name = username, password = password)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                // Dismiss before showing the error so the two windows do not
                // overlap. The finally block dismisses again, which is a no-op.
                progress.dismiss()
                showAuthError(
                    logMessage = "Failed to create new account",
                    e = e,
                    fallbackMessage = getString(R.string.failed_to_create_new_account),
                )
                return@launch
            }

            val response = try {
                api().signIn(
                    username = user.name,
                    password = password,
                    label = tokenLabel(),
                )
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                // Dismiss before showing the sign-in dialog so the two windows do
                // not overlap. The finally block dismisses again, which is a no-op.
                progress.dismiss()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.account_created_sign_in_failed),
                    Toast.LENGTH_LONG,
                ).show()
                showSignInDialog(onComplete, prefilledUsername = user.name)
                return@launch
            }

            progress.dismiss()
            completeSignIn(response, onComplete)
        } finally {
            progress.dismiss()
        }
    }
}

private fun Fragment.signIn(username: String, password: String, onComplete: () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        val progress = showProgressDialog(R.string.loading)

        try {
            val response = try {
                api().signIn(
                    username = username,
                    password = password,
                    label = tokenLabel(),
                )
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                // Dismiss before showing the error so the two windows do not
                // overlap. The finally block dismisses again, which is a no-op.
                progress.dismiss()
                showAuthError(
                    logMessage = "Sign in failed",
                    e = e,
                    fallbackMessage = getString(R.string.failed_to_sign_in),
                )
                return@launch
            }

            progress.dismiss()
            completeSignIn(response, onComplete)
        } finally {
            progress.dismiss()
        }
    }
}

private suspend fun Fragment.completeSignIn(response: CreateTokenResponse, onComplete: () -> Unit) {
    try {
        storeSignedInSession(db = db(), prefs = prefs, response = response)
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        showAuthError(
            logMessage = "Failed to store signed-in account",
            e = e,
            fallbackMessage = getString(R.string.failed_to_sign_in),
        )
        return
    }

    // The session is durable now. If the view was destroyed in the meantime the
    // coroutine is cancelled before the callback runs, so [onComplete] may be
    // skipped; the account is still signed in and the next screen sees it.
    val toastContext = context
    if (toastContext != null) {
        Toast.makeText(
            toastContext,
            getString(R.string.logged_in_as, response.user.name),
            Toast.LENGTH_SHORT,
        ).show()
    }

    try {
        onComplete()
    } catch (e: Throwable) {
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

private fun Fragment.showProgressDialog(@StringRes message: Int): AlertDialog {
    val dialogView = layoutInflater.inflate(R.layout.account_progress_dialog, null)
    dialogView.findViewById<TextView>(R.id.progressMessage).setText(message)

    return MaterialAlertDialogBuilder(requireContext())
        .setView(dialogView)
        .setCancelable(false)
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

private fun tokenLabel() = "BTC Map Android ${BuildConfig.VERSION_CODE}"

private const val AUTH_TAG = "auth"
