package org.btcmap.auth

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.btcmap.R

/**
 * Shows a sign-in or sign-up failure.
 *
 * It is a [DialogFragment], not a plain dialog, so the message survives a
 * configuration change: the previous implementation dismissed the dialog with
 * the view and the one-shot [AuthEvent.Failed] was already consumed, so a
 * rotation lost the error entirely.
 */
internal class AuthErrorDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error)
            .setMessage(requireArguments().getString(ARG_MESSAGE))
            .setPositiveButton(android.R.string.ok, null)
            .create()

    companion object {
        const val TAG = "auth-error"

        private const val ARG_MESSAGE = "arg-message"

        fun newInstance(message: String): AuthErrorDialogFragment =
            AuthErrorDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
    }
}
