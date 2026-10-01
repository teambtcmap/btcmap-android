package org.btcmap.auth

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.btcmap.R
import org.btcmap.ui.AuthChooserComposeView
import org.btcmap.ui.AuthChooserOption

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
        val view = AuthChooserComposeView(requireContext()).apply {
            setViewTreeLifecycleOwner(this@AuthChooserDialogFragment)
            setViewTreeSavedStateRegistryOwner(this@AuthChooserDialogFragment)
            setViewTreeViewModelStoreOwner(this@AuthChooserDialogFragment)
            options = listOf(
                AuthChooserOption(
                    key = SIGN_UP,
                    title = getString(R.string.i_don_t_have_an_account),
                    subtitle = getString(R.string.create_account_subtitle),
                ),
                AuthChooserOption(
                    key = SIGN_IN,
                    title = getString(R.string.log_in_with_existing_account),
                    subtitle = getString(R.string.sign_in_subtitle),
                ),
            )
            onSelect = { key -> showCredentials(if (key == SIGN_UP) AuthMode.SignUp else AuthMode.SignIn) }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun showCredentials(mode: AuthMode) {
        dismiss()
        AuthDialogFragment.newCredentials(mode = mode, extras = arguments?.getBundle(ARG_EXTRAS))
            .show(parentFragmentManager, AuthDialogFragment.TAG)
    }

    companion object {
        const val TAG = "auth-chooser"

        private const val ARG_EXTRAS = "arg-extras"
        private const val SIGN_UP = "sign-up"
        private const val SIGN_IN = "sign-in"

        fun newInstance(extras: Bundle?): AuthChooserDialogFragment =
            AuthChooserDialogFragment().apply {
                arguments = Bundle().apply { extras?.let { putBundle(ARG_EXTRAS, it) } }
            }
    }
}
