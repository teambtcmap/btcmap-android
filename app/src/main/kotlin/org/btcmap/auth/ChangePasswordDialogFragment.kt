package org.btcmap.auth

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import org.btcmap.R
import org.btcmap.ui.AuthField
import org.btcmap.ui.AuthImeAction

/**
 * Collects the current and new password for the change-password form.
 *
 * Like [AuthDialogFragment], the submitted passwords are handed to the host
 * through a host-scoped [AuthFormResultViewModel] under [REQUEST_KEY] (the
 * result bundle only signals that the form is ready), and the typed values live
 * in [AuthFormViewModel], retained across a rotation but never saved.
 */
internal class ChangePasswordDialogFragment : AuthFormDialogFragment() {

    private val formState: AuthFormViewModel by lazy {
        ViewModelProvider(this)[AuthFormViewModel::class.java]
    }

    private val formResults: AuthFormResultViewModel by lazy { hostAuthFormResults() }

    override val titleRes: Int get() = R.string.change_password

    override val positiveRes: Int get() = R.string.save

    override fun fields(): List<AuthField> = listOf(
        AuthField(
            key = CURRENT,
            label = getString(R.string.current_password),
            value = formState.currentPassword,
            isPassword = true,
            imeAction = AuthImeAction.Next,
        ),
        AuthField(
            key = NEW,
            label = getString(R.string.new_password),
            value = formState.password,
            isPassword = true,
            helper = getString(R.string.password_min_length, AuthValidation.MIN_PASSWORD_LENGTH),
            imeAction = AuthImeAction.Next,
        ),
        AuthField(
            key = CONFIRMATION,
            label = getString(R.string.confirm_password),
            value = formState.confirmation,
            isPassword = true,
            imeAction = AuthImeAction.Done,
        ),
    )

    override fun validate(values: Map<String, String>): List<AuthError> = AuthValidation.changePassword(
        current = values[CURRENT].orEmpty(),
        new = values[NEW].orEmpty(),
        confirmation = values[CONFIRMATION].orEmpty(),
    )

    override fun onSubmit(values: Map<String, String>) {
        val current = values[CURRENT].orEmpty()
        val new = values[NEW].orEmpty()

        // The passwords are handed to the host now; do not keep them in memory.
        formState.currentPassword = ""
        formState.password = ""
        formState.confirmation = ""

        // Held in a host-scoped view model, not the result bundle, so a pending
        // result cannot persist the passwords to saved instance state.
        formResults.changePassword = ChangePasswordCredentials(
            currentPassword = current,
            newPassword = new,
        )

        parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle())
    }

    override fun onValuesChanged(values: Map<String, String>) {
        formState.currentPassword = values[CURRENT].orEmpty()
        formState.password = values[NEW].orEmpty()
        formState.confirmation = values[CONFIRMATION].orEmpty()
    }

    override fun fieldKeyFor(error: AuthError): String = when (error) {
        AuthError.CurrentPasswordRequired -> CURRENT
        AuthError.PasswordRequired, AuthError.PasswordTooShort -> NEW
        AuthError.PasswordsDoNotMatch -> CONFIRMATION
        // Not raised by this form.
        AuthError.UsernameRequired -> CURRENT
    }

    override fun errorMessage(error: AuthError): String = when (error) {
        AuthError.CurrentPasswordRequired, AuthError.UsernameRequired, AuthError.PasswordRequired ->
            getString(R.string.field_required)

        AuthError.PasswordTooShort ->
            getString(R.string.password_min_length, AuthValidation.MIN_PASSWORD_LENGTH)

        AuthError.PasswordsDoNotMatch -> getString(R.string.passwords_do_not_match)
    }

    companion object {
        const val TAG = "change-password-dialog"

        /**
         * Result key of a submitted form; the passwords themselves are held in
         * [AuthFormResultViewModel]. See `registerChangePasswordResultListener`.
         */
        const val REQUEST_KEY = "org.btcmap.auth.change-password"

        private const val CURRENT = "current"
        private const val NEW = "new"
        private const val CONFIRMATION = "confirmation"

        fun newInstance(): ChangePasswordDialogFragment = ChangePasswordDialogFragment()
    }
}
