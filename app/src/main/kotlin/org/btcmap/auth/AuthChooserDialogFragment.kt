package org.btcmap.auth

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.btcmap.R

/**
 * Asks whether the user wants to create an account or sign in, then opens the
 * matching credentials form.
 *
 * It is a [DialogFragment] so the chooser is restored rather than dismissed on a
 * configuration change. The caller's [extras] are carried through to the
 * credentials form, which echoes them back to the caller so the action that
 * needed the account can be resumed.
 */
internal class AuthChooserDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = layoutInflater.inflate(R.layout.account_choices_dialog, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialogView.findViewById<View>(R.id.createAccountOption).setOnClickListener {
            showCredentials(AuthMode.SignUp)
        }
        dialogView.findViewById<View>(R.id.signInOption).setOnClickListener {
            showCredentials(AuthMode.SignIn)
        }

        return dialog
    }

    private fun showCredentials(mode: AuthMode) {
        dismiss()
        AuthDialogFragment.newCredentials(mode = mode, extras = arguments?.getBundle(ARG_EXTRAS))
            .show(parentFragmentManager, AuthDialogFragment.TAG)
    }

    companion object {
        const val TAG = "auth-chooser"

        private const val ARG_EXTRAS = "arg-extras"

        fun newInstance(extras: Bundle?): AuthChooserDialogFragment =
            AuthChooserDialogFragment().apply {
                arguments = Bundle().apply { extras?.let { putBundle(ARG_EXTRAS, it) } }
            }
    }
}
