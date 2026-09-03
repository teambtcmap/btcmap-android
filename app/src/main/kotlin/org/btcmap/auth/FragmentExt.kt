package org.btcmap.auth

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.api
import org.btcmap.db
import org.btcmap.db.table.user.User
import org.btcmap.settings.authToken
import org.btcmap.settings.prefs

fun Fragment.showAuthDialog(message: String, onSuccess: () -> Unit) {
    if (message.isBlank()) {
        showAccountChoicesDialog(onSuccess)
    } else {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                showAccountChoicesDialog(onSuccess)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

private fun Fragment.showAccountChoicesDialog(onSuccess: () -> Unit) {
    val dialogView = layoutInflater.inflate(R.layout.account_choices_dialog, null)
    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.account)
        .setView(dialogView)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialogView.findViewById<View>(R.id.createAccountOption).setOnClickListener {
        dialog.dismiss()
        createNewAccount(onSuccess)
    }
    dialogView.findViewById<View>(R.id.signInOption).setOnClickListener {
        dialog.dismiss()
        showSignInDialog(onSuccess)
    }

    dialog.show()
}

private fun Fragment.createNewAccount(onComplete: () -> Unit) {
    val dialogView = layoutInflater.inflate(R.layout.account_dialog, null)
    val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
    val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.passwordInput)

    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.new_account)
        .setView(dialogView)
        .setPositiveButton(R.string.sign_up, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()

            var valid = true
            if (username.isEmpty()) {
                usernameInput.error = getString(R.string.field_required)
                valid = false
            }
            if (password.isEmpty()) {
                passwordInput.error = getString(R.string.field_required)
                valid = false
            }
            if (!valid) return@setOnClickListener

            signUp(username, password, onComplete)
            dialog.dismiss()
        }
    }

    dialog.show()
}

private fun Fragment.signUp(username: String, password: String, onComplete: () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val user = api().createUser(name = username, password = password)
            val token = api().signIn(
                username = user.name,
                password = password,
                label = "BTC Map Android ${BuildConfig.VERSION_CODE}",
            )
            prefs.authToken = token.token
            db().user.insert(
                User(
                    id = user.id,
                    name = user.name,
                    roles = user.roles,
                    savedPlaces = user.savedPlaces,
                    savedAreas = user.savedAreas,
                )
            )
            Toast.makeText(
                requireContext(),
                getString(R.string.logged_in_as, user.name),
                Toast.LENGTH_SHORT,
            ).show()
            onComplete()
        } catch (e: Exception) {
            Log.e("auth", "Failed to create new account", e)
            val message = e.message?.takeIf { it.isNotBlank() }
                ?: getString(R.string.failed_to_create_new_account)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}

private fun Fragment.showSignInDialog(onComplete: () -> Unit) {
    val dialogView = layoutInflater.inflate(R.layout.account_dialog, null)
    val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
    val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.passwordInput)

    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.login)
        .setView(dialogView)
        .setPositiveButton(R.string.login, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()

            var valid = true
            if (username.isEmpty()) {
                usernameInput.error = getString(R.string.field_required)
                valid = false
            }
            if (password.isEmpty()) {
                passwordInput.error = getString(R.string.field_required)
                valid = false
            }
            if (!valid) return@setOnClickListener

            signIn(username, password, onComplete)
            dialog.dismiss()
        }
    }

    dialog.show()
}

private fun Fragment.signIn(username: String, password: String, onComplete: () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val signInRes = api().signIn(
                username,
                password,
                "BTC Map Android ${BuildConfig.VERSION_CODE}"
            )
            prefs.authToken = signInRes.token
            db().user.insert(
                User(
                    id = signInRes.user.id,
                    name = signInRes.user.name,
                    roles = signInRes.user.roles,
                    savedPlaces = signInRes.user.savedPlaces,
                    savedAreas = signInRes.user.savedAreas,
                )
            )
            onComplete()
        } catch (e: Throwable) {
            Log.e("auth", "Sign in failed", e)
            val message = e.message?.takeIf { it.isNotBlank() }
                ?: getString(R.string.failed_to_create_new_account)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}
