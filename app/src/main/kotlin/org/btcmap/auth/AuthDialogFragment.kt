package org.btcmap.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import org.btcmap.R

internal enum class AuthMode {
    SignIn,
    SignUp,
}

/**
 * Collects credentials for sign-in and sign-up.
 *
 * The submitted credentials are reported through the Fragment Result API under
 * [REQUEST_KEY], so they reach the (possibly recreated) caller through a
 * listener that caller registers in its own lifecycle, rather than a callback
 * held by this fragment. [EXTRAS] is echoed back untouched so the caller can
 * resume the action that needed the account.
 *
 * The typed passwords are kept in [AuthFormViewModel] and the password fields
 * opt out of view-state saving, so they survive a rotation without being written
 * to saved instance state.
 */
internal class AuthDialogFragment : AuthFormDialogFragment() {

    private val formState: AuthFormViewModel by lazy {
        ViewModelProvider(this)[AuthFormViewModel::class.java]
    }

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

        // The credentials are handed to the caller now; do not keep them in memory.
        formState.password = ""
        formState.confirmation = ""

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(MODE, mode.name)
                putString(USERNAME, username)
                putString(PASSWORD, password)
                arguments?.getBundle(ARG_EXTRAS)?.let { putBundle(EXTRAS, it) }
            },
        )
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

        /** Result key of the submitted credentials; see `registerAuthResultListener`. */
        const val REQUEST_KEY = "org.btcmap.auth.credentials"

        const val MODE = "mode"
        const val USERNAME = "username"
        const val PASSWORD = "password"
        const val EXTRAS = "extras"

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
