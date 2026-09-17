package org.btcmap.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import org.btcmap.R

/**
 * Collects the current and new password for the change-password form.
 *
 * Like [AuthDialogFragment], the submitted passwords are reported through the
 * Fragment Result API under [REQUEST_KEY], and the typed passwords live in
 * [AuthFormViewModel] with the password fields opting out of view-state saving,
 * so the form survives a rotation without persisting the passwords.
 */
internal class ChangePasswordDialogFragment : AuthFormDialogFragment() {

    private val formState: AuthFormViewModel by lazy {
        ViewModelProvider(this)[AuthFormViewModel::class.java]
    }

    private lateinit var currentInput: TextInputEditText
    private lateinit var newInput: TextInputEditText
    private lateinit var confirmationInput: TextInputEditText

    override val titleRes: Int get() = R.string.change_password

    override val positiveRes: Int get() = R.string.save

    override val currentPasswordField: TextInputEditText get() = currentInput

    override val passwordField: TextInputEditText get() = newInput

    override val confirmationField: TextInputEditText get() = confirmationInput

    override val doneField: TextInputEditText get() = confirmationInput

    override fun createFormView(inflater: LayoutInflater): View {
        val view = inflater.inflate(R.layout.change_password_dialog, null)
        currentInput = view.findViewById(R.id.currentPasswordInput)
        newInput = view.findViewById(R.id.newPasswordInput)
        confirmationInput = view.findViewById(R.id.confirmPasswordInput)

        currentInput.doAfterTextChanged { formState.currentPassword = it?.toString().orEmpty() }
        newInput.doAfterTextChanged { formState.password = it?.toString().orEmpty() }
        confirmationInput.doAfterTextChanged {
            formState.confirmation = it?.toString().orEmpty()
        }

        return view
    }

    override fun restoreFormState() {
        currentInput.setText(formState.currentPassword)
        newInput.setText(formState.password)
        confirmationInput.setText(formState.confirmation)
    }

    override fun validate(): List<AuthError> = AuthValidation.changePassword(
        current = currentInput.text.toString(),
        new = newInput.text.toString(),
        confirmation = confirmationInput.text.toString(),
    )

    override fun onSubmit() {
        val current = currentInput.text.toString()
        val new = newInput.text.toString()

        // The passwords are handed to the caller now; do not keep them in memory.
        formState.currentPassword = ""
        formState.password = ""
        formState.confirmation = ""

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(CURRENT_PASSWORD, current)
                putString(NEW_PASSWORD, new)
            },
        )
    }

    companion object {
        const val TAG = "change-password-dialog"

        /** Result key of the submitted passwords; see `registerChangePasswordResultListener`. */
        const val REQUEST_KEY = "org.btcmap.auth.change-password"

        const val CURRENT_PASSWORD = "current-password"
        const val NEW_PASSWORD = "new-password"

        fun newInstance(): ChangePasswordDialogFragment = ChangePasswordDialogFragment()
    }
}
