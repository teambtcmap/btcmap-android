package org.btcmap.auth

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.btcmap.R

/**
 * Collects the current and new password for the change-password form.
 *
 * This is a [DialogFragment] rather than a plain `AlertDialog` for the same
 * reason as [AuthDialogFragment]: the typed passwords are part of the dialog's
 * saved view hierarchy state, so they survive a configuration change instead of
 * being thrown away. The host provides [onSubmit]; after process death the
 * dialog is restored without a handler and simply dismisses on submit.
 */
internal class ChangePasswordDialogFragment : DialogFragment() {

    var onSubmit: ((current: String, new: String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = layoutInflater.inflate(R.layout.change_password_dialog, null)
        val currentInput = dialogView.findViewById<TextInputEditText>(R.id.currentPasswordInput)
        val newInput = dialogView.findViewById<TextInputEditText>(R.id.newPasswordInput)
        val confirmationInput =
            dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordInput)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_password)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val current = currentInput.text.toString()
                val new = newInput.text.toString()
                val confirmation = confirmationInput.text.toString()

                val errors = AuthValidation.changePassword(current, new, confirmation)
                if (errors.isNotEmpty()) {
                    showErrors(errors, currentInput, newInput, confirmationInput)
                    return@setOnClickListener
                }

                dismiss()

                val handler = onSubmit
                if (handler == null) {
                    // Only reachable after process death, where the callback
                    // cannot be restored; the user has to start over.
                    Log.w(TAG, "Change-password dialog has no submit handler; dropping input")
                } else {
                    handler(current, new)
                }
            }
        }

        return dialog
    }

    private fun showErrors(
        errors: List<ChangePasswordError>,
        currentInput: TextInputEditText,
        newInput: TextInputEditText,
        confirmationInput: TextInputEditText,
    ) {
        // Clear errors from the previous attempt first, so a corrected field
        // does not keep showing an error that no longer applies.
        currentInput.error = null
        newInput.error = null
        confirmationInput.error = null

        if (ChangePasswordError.CurrentRequired in errors) {
            currentInput.error = getString(R.string.field_required)
        }
        when {
            ChangePasswordError.NewRequired in errors ->
                newInput.error = getString(R.string.field_required)

            ChangePasswordError.NewTooShort in errors ->
                newInput.error = getString(R.string.password_min_length)
        }
        if (ChangePasswordError.ConfirmationMismatch in errors) {
            confirmationInput.error = getString(R.string.passwords_do_not_match)
        }
    }

    companion object {
        const val TAG = "change-password-dialog"

        fun newInstance(): ChangePasswordDialogFragment = ChangePasswordDialogFragment()
    }
}
