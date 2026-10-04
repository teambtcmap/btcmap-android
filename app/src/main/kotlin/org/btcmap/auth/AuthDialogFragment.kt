package org.btcmap.auth

import android.os.Bundle
import androidx.compose.ui.autofill.ContentType
import androidx.lifecycle.ViewModelProvider
import org.btcmap.R
import org.btcmap.ui.AuthField
import org.btcmap.ui.AuthImeAction

internal enum class AuthMode {
    SignIn,
    SignUp,
}

/**
 * Collects credentials for sign-in and sign-up.
 *
 * The submitted credentials are handed to the host through a host-scoped
 * [AuthFormResultViewModel] under [REQUEST_KEY]: the values stay in memory while
 * the result bundle carries only a signal that a form is ready. The host
 * registers a listener for [REQUEST_KEY] in its own lifecycle, so the
 * credentials reach it even when it is recreated by a configuration change. The
 * extras given to [newCredentials] are carried through untouched, so the caller
 * can resume the action that needed the account.
 *
 * The typed values are kept in [AuthFormViewModel], which is retained across a
 * configuration change but never saved, so they survive a rotation without the
 * passwords being written to saved instance state.
 */
internal class AuthDialogFragment : AuthFormDialogFragment() {

    private val formState: AuthFormViewModel by lazy {
        ViewModelProvider(this)[AuthFormViewModel::class.java]
    }

    private val formResults: AuthFormResultViewModel by lazy { hostAuthFormResults() }

    override val titleRes: Int
        get() = if (signUp()) R.string.new_account else R.string.login

    override val positiveRes: Int
        get() = if (signUp()) R.string.sign_up else R.string.login

    private fun signUp(): Boolean = mode() == AuthMode.SignUp

    override fun fields(): List<AuthField> {
        // A prefilled username seeds the retained value once.
        if (formState.username.isEmpty()) {
            formState.username = requireArguments().getString(ARG_USERNAME).orEmpty()
        }

        val list = mutableListOf(
            AuthField(
                key = USERNAME,
                label = getString(R.string.username),
                value = formState.username,
                isPassword = false,
                imeAction = AuthImeAction.Next,
                contentType = if (signUp()) ContentType.NewUsername else ContentType.Username,
            ),
            AuthField(
                key = PASSWORD,
                label = getString(R.string.password),
                value = formState.password,
                isPassword = true,
                helper = if (signUp()) {
                    getString(R.string.password_min_length, AuthValidation.MIN_PASSWORD_LENGTH)
                } else {
                    null
                },
                imeAction = if (signUp()) AuthImeAction.Next else AuthImeAction.Done,
                contentType = if (signUp()) ContentType.NewPassword else ContentType.Password,
            ),
        )

        if (signUp()) {
            list.add(
                AuthField(
                    key = CONFIRMATION,
                    label = getString(R.string.confirm_password),
                    value = formState.confirmation,
                    isPassword = true,
                    imeAction = AuthImeAction.Done,
                    contentType = ContentType.NewPassword,
                )
            )
        }

        return list
    }

    override fun validate(values: Map<String, String>): List<AuthError> {
        val username = values[USERNAME].orEmpty().trim()
        val password = values[PASSWORD].orEmpty()
        val confirmation = values[CONFIRMATION].orEmpty()

        return if (signUp()) {
            AuthValidation.signUp(username, password, confirmation)
        } else {
            AuthValidation.signIn(username, password)
        }
    }

    override fun onSubmit(values: Map<String, String>) {
        val mode = mode()
        val username = values[USERNAME].orEmpty().trim()
        val password = values[PASSWORD].orEmpty()

        // The credentials are handed to the host now; do not keep them in memory.
        formState.username = ""
        formState.password = ""
        formState.confirmation = ""

        // Held in a host-scoped view model, not the result bundle: a pending
        // result is written to saved instance state while no started listener
        // has consumed it, which would persist the password. The result only
        // signals that a form was submitted; see `registerAuthResultListener`.
        formResults.credentials = AuthCredentials(
            mode = mode,
            username = username,
            password = password,
            extras = arguments?.getBundle(ARG_EXTRAS) ?: Bundle(),
        )

        parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle())
    }

    override fun onValuesChanged(values: Map<String, String>) {
        formState.username = values[USERNAME].orEmpty()
        formState.password = values[PASSWORD].orEmpty()
        formState.confirmation = values[CONFIRMATION].orEmpty()
    }

    override fun fieldKeyFor(error: AuthError): String = when (error) {
        AuthError.UsernameRequired -> USERNAME
        AuthError.PasswordRequired, AuthError.PasswordTooShort -> PASSWORD
        AuthError.PasswordsDoNotMatch -> CONFIRMATION
        // Not raised by this form.
        AuthError.CurrentPasswordRequired -> USERNAME
    }

    override fun errorMessage(error: AuthError): String = when (error) {
        AuthError.UsernameRequired, AuthError.CurrentPasswordRequired, AuthError.PasswordRequired ->
            getString(R.string.field_required)

        AuthError.PasswordTooShort ->
            getString(R.string.password_min_length, AuthValidation.MIN_PASSWORD_LENGTH)

        AuthError.PasswordsDoNotMatch -> getString(R.string.passwords_do_not_match)
    }

    private fun mode(): AuthMode {
        val name = requireArguments().getString(ARG_MODE) ?: AuthMode.SignIn.name
        return try {
            AuthMode.valueOf(name)
        } catch (e: IllegalArgumentException) {
            AuthMode.SignIn
        }
    }

    companion object {
        const val TAG = "auth-dialog"

        /**
         * Result key of a submitted form; the credentials themselves are held in
         * [AuthFormResultViewModel]. See `registerAuthResultListener`.
         */
        const val REQUEST_KEY = "org.btcmap.auth.credentials"

        private const val ARG_MODE = "arg-mode"
        private const val ARG_USERNAME = "arg-username"
        private const val ARG_EXTRAS = "arg-extras"

        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val CONFIRMATION = "confirmation"

        fun newCredentials(
            mode: AuthMode,
            extras: Bundle? = null,
            prefilledUsername: String? = null,
        ): AuthDialogFragment = AuthDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MODE, mode.name)
                prefilledUsername?.let { putString(ARG_USERNAME, it) }
                extras?.let { putBundle(ARG_EXTRAS, it) }
            }
        }
    }
}
