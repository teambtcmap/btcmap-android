package org.btcmap.auth

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.db
import org.btcmap.settings.prefs
import org.btcmap.util.userFacingMessage

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
 * change also receives a form submitted afterwards. The request runs in a
 * retained [AuthViewModel], so it survives a rotation and its outcome reaches
 * the recreated fragment. [onAuthenticated] runs only once the session has been
 * stored, with the [extras] given to [showAuthDialog].
 */
fun Fragment.registerAuthResultListener(onAuthenticated: (extras: Bundle) -> Unit) {
    val viewModel = ViewModelProvider(
        this,
        AuthViewModel.Factory(api = api(), db = db(), prefs = prefs),
    )[AuthViewModel::class.java]

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
            AuthMode.SignIn -> viewModel.signIn(username, password, extras)
            AuthMode.SignUp -> viewModel.signUp(username, password, extras)
        }
    }

    // Kept outside the collectors so a stop/start of the view does not lose it
    // and show a second progress dialog.
    var progress: AlertDialog? = null

    fun dismissProgress() {
        progress?.dismiss()
        progress = null
    }

    // The busy state follows the view, not STARTED, so a request that finishes
    // while the app is in the background still dismisses its dialog.
    viewLifecycleOwner.lifecycleScope.launch {
        viewModel.busy.collect { busy ->
            if (busy) {
                if (progress == null) {
                    progress = showProgressDialog(R.string.loading) { viewModel.cancel() }
                }
            } else {
                dismissProgress()
            }
        }
    }

    // Outcomes are handled only while the view is visible; anything queued while
    // it is not is buffered by the view model and delivered on return.
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                dismissProgress()
                handleAuthEvent(event, onAuthenticated)
            }
        }
    }
}

private fun Fragment.handleAuthEvent(
    event: AuthEvent,
    onAuthenticated: (extras: Bundle) -> Unit,
) {
    when (event) {
        is AuthEvent.Authenticated -> {
            Toast.makeText(
                requireContext(),
                getString(R.string.logged_in_as, event.name),
                Toast.LENGTH_SHORT,
            ).show()
            onAuthenticated(event.extras)
        }

        is AuthEvent.AccountCreated -> {
            Toast.makeText(
                requireContext(),
                getString(R.string.account_created_sign_in_failed),
                Toast.LENGTH_LONG,
            ).show()
            AuthDialogFragment.newCredentials(
                mode = AuthMode.SignIn,
                extras = event.extras,
                prefilledUsername = event.username,
            ).show(childFragmentManager, AuthDialogFragment.TAG)
        }

        is AuthEvent.Failed -> showAuthError(
            e = event.error,
            fallbackMessage = getString(
                when (event.operation) {
                    AuthOperation.SignIn -> R.string.failed_to_sign_in
                    AuthOperation.SignUp -> R.string.failed_to_create_new_account
                },
            ),
        )
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

private fun Fragment.showAuthError(e: Throwable, fallbackMessage: String) {
    if (!isAdded) return

    // Shown as a fragment so it is restored with the screen: the failure event
    // is delivered once, so a plain dialog dismissed on rotation would lose it.
    AuthErrorDialogFragment.newInstance(e.userFacingMessage(fallbackMessage))
        .show(childFragmentManager, AuthErrorDialogFragment.TAG)
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
