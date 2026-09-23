package org.btcmap.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import org.btcmap.R
import org.btcmap.util.setFieldHelperText

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
 * The typed passwords are kept in [AuthFormViewModel] and the password fields
 * opt out of view-state saving, so they survive a rotation without being written
 * to saved instance state.
 */
internal class AuthDialogFragment : AuthFormDialogFragment() {

    private val formState: AuthFormViewModel by lazy {
        ViewModelProvider(this)[AuthFormViewModel::class.java]
    }

    private val formResults: AuthFormResultViewModel by lazy { hostAuthFormResults() }

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private var confirmationInput: TextInputEditText? = null

    override val titleRes: Int
        get() = if (signUp()) R.string.new_account else R.string.login

    override val positiveRes: Int
        get() = if (signUp()) R.string.sign_up else R.string.login

    override val usernameField: TextInputEditText get() = usernameInput

    override val passwordField: TextInputEditText get() = passwordInput

    override val confirmationField: TextInputEditText? get() = confirmationInput

    override val doneField: TextInputEditText
        get() = confirmationInput ?: passwordInput

    private fun signUp(): Boolean = mode() == AuthMode.SignUp

    // The form is the content of a MaterialAlertDialog, which has no parent view
    // to inflate into, so its root layout params are intentionally unused.
    @SuppressLint("InflateParams")
    override fun createFormView(inflater: LayoutInflater): View {
        val signUp = signUp()
        val view = inflater.inflate(
            if (signUp) R.layout.account_sign_up_dialog else R.layout.account_dialog,
            null,
        )
        usernameInput = view.findViewById(R.id.usernameInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        confirmationInput =
            if (signUp) view.findViewById<TextInputEditText>(R.id.confirmPasswordInput) else null

        if (signUp) {
            passwordInput.setFieldHelperText(
                getString(R.string.password_min_length, AuthValidation.MIN_PASSWORD_LENGTH),
            )
        }

        requireArguments().getString(ARG_USERNAME)?.let(usernameInput::setText)

        passwordInput.doAfterTextChanged { formState.password = it?.toString().orEmpty() }
        confirmationInput?.doAfterTextChanged {
            formState.confirmation = it?.toString().orEmpty()
        }

        return view
    }

    override fun restoreFormState() {
        passwordInput.setText(formState.password)
        confirmationInput?.setText(formState.confirmation)
    }

    override fun validate(): List<AuthError> {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmation = confirmationInput?.text?.toString().orEmpty()

        return if (signUp()) {
            AuthValidation.signUp(username, password, confirmation)
        } else {
            AuthValidation.signIn(username, password)
        }
    }

    override fun onSubmit() {
        val mode = mode()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()

        // The credentials are handed to the host now; do not keep them in memory.
        formState.password = ""
        formState.confirmation = ""

        // Held in a host-scoped view model, not the result bundle: a pending
        // result is written to saved instance state while no started listener
        // has consumed it, which would persist the password. The result only
        // signals that a form was submitted; see `registerAuthResultListener`.
        formResults.pending = AuthFormResult.Credentials(
            mode = mode,
            username = username,
            password = password,
            extras = arguments?.getBundle(ARG_EXTRAS) ?: Bundle(),
        )

        parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle())
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
