package org.btcmap.auth

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.btcmap.R

/**
 * Shared scaffolding for the credential dialogs: the positive/negative buttons,
 * submitting from the keyboard, and showing [AuthError]s on the matching fields.
 *
 * A subclass only inflates the form, validates its input and reports the result.
 * The form is a [DialogFragment] so it is recreated with the screen on a
 * configuration change; the submitted values are delivered through the Fragment
 * Result API by the subclass instead of a callback held by this fragment, which
 * would not survive that recreation.
 */
internal abstract class AuthFormDialogFragment : DialogFragment() {

    @get:StringRes
    protected abstract val titleRes: Int

    @get:StringRes
    protected abstract val positiveRes: Int

    /** Inflates the form and binds the subclass's field properties. */
    protected abstract fun createFormView(inflater: LayoutInflater): View

    /** Validates the fields, returning every problem found. */
    protected abstract fun validate(): List<AuthError>

    /** Reports the collected input; runs only when [validate] returns no errors. */
    protected abstract fun onSubmit()

    /** Restores text that must survive a configuration change but not process death. */
    protected open fun restoreFormState() = Unit

    /** The field whose Done action submits the form. */
    protected abstract val doneField: TextInputEditText

    protected open val usernameField: TextInputEditText? get() = null

    protected open val currentPasswordField: TextInputEditText? get() = null

    protected abstract val passwordField: TextInputEditText

    protected open val confirmationField: TextInputEditText? get() = null

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val form = createFormView(layoutInflater)
        restoreFormState()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(form)
            .setPositiveButton(positiveRes, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { submit() }
            doneField.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit()
                    true
                } else {
                    false
                }
            }
        }

        return dialog
    }

    private fun submit() {
        val errors = validate()
        if (errors.isEmpty()) {
            onSubmit()
            dismiss()
            return
        }
        showErrors(errors)
    }

    private fun showErrors(errors: List<AuthError>) {
        // Clear errors from the previous attempt first, so a corrected field
        // does not keep showing an error that no longer applies.
        usernameField?.error = null
        currentPasswordField?.error = null
        passwordField.error = null
        confirmationField?.error = null

        if (AuthError.UsernameRequired in errors) {
            usernameField?.error = getString(R.string.field_required)
        }
        if (AuthError.CurrentPasswordRequired in errors) {
            currentPasswordField?.error = getString(R.string.field_required)
        }
        when {
            AuthError.PasswordRequired in errors -> {
                passwordField.error = getString(R.string.field_required)
            }

            AuthError.PasswordTooShort in errors -> {
                passwordField.error = getString(R.string.password_min_length)
            }
        }
        if (AuthError.PasswordsDoNotMatch in errors) {
            confirmationField?.error = getString(R.string.passwords_do_not_match)
        }
    }
}
