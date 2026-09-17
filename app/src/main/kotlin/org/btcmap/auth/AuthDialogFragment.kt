package org.btcmap.auth

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.btcmap.R

internal enum class AuthMode {
    SignIn,
    SignUp,
}

/**
 * Collects credentials for sign-in and sign-up.
 *
 * This is a [DialogFragment] rather than a plain `AlertDialog` so the typed
 * username and password survive a configuration change instead of being
 * thrown away. The host provides [onSubmit]; because a fragment instance is
 * retained across configuration changes, the callback and the entered text both
 * survive a rotation. After process death the dialog is restored without a
 * handler and simply dismisses on submit instead of silently doing nothing.
 */
internal class AuthDialogFragment : DialogFragment() {

    var onSubmit: ((mode: AuthMode, username: String, password: String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val signUp = mode() == AuthMode.SignUp

        val dialogView = layoutInflater.inflate(
            if (signUp) R.layout.account_sign_up_dialog else R.layout.account_dialog,
            null,
        )
        val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.passwordInput)
        val confirmationInput =
            if (signUp) dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordInput) else null

        requireArguments().getString(ARG_USERNAME)?.let(usernameInput::setText)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (signUp) R.string.new_account else R.string.login)
            .setView(dialogView)
            .setPositiveButton(if (signUp) R.string.sign_up else R.string.login, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString()
                val confirmation = confirmationInput?.text?.toString().orEmpty()

                val errors = if (signUp) {
                    AuthValidation.signUp(username, password, confirmation)
                } else {
                    AuthValidation.signIn(username, password)
                }
                if (errors.isNotEmpty()) {
                    showErrors(errors, usernameInput, passwordInput, confirmationInput)
                    return@setOnClickListener
                }

                dismiss()

                val handler = onSubmit
                if (handler == null) {
                    // Only reachable after process death, where the callback
                    // cannot be restored; the user has to start over.
                    Log.w(TAG, "Auth dialog has no submit handler; dropping credentials")
                } else {
                    handler(mode(), username, password)
                }
            }
        }

        return dialog
    }

    private fun mode(): AuthMode {
        val name = requireArguments().getString(ARG_MODE) ?: AuthMode.SignIn.name
        return runCatching { AuthMode.valueOf(name) }.getOrDefault(AuthMode.SignIn)
    }

    private fun showErrors(
        errors: List<AuthError>,
        usernameInput: TextInputEditText,
        passwordInput: TextInputEditText,
        confirmationInput: TextInputEditText?,
    ) {
        if (AuthError.UsernameRequired in errors) {
            usernameInput.error = getString(R.string.field_required)
        }

        when {
            AuthError.PasswordRequired in errors -> {
                passwordInput.error = getString(R.string.field_required)
            }

            AuthError.PasswordTooShort in errors -> {
                passwordInput.error = getString(R.string.password_min_length)
            }
        }

        if (AuthError.PasswordsDoNotMatch in errors) {
            confirmationInput?.error = getString(R.string.passwords_do_not_match)
        }
    }

    companion object {
        const val TAG = "auth-dialog"

        private const val ARG_MODE = "mode"
        private const val ARG_USERNAME = "username"

        fun newCredentials(mode: AuthMode, prefilledUsername: String? = null): AuthDialogFragment =
            AuthDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode.name)
                    prefilledUsername?.let { putString(ARG_USERNAME, it) }
                }
            }
    }
}
