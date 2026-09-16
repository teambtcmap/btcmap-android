package org.btcmap.auth

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.CreateTokenResponse
import org.btcmap.api.createUser
import org.btcmap.api.signIn
import org.btcmap.db
import org.btcmap.db.table.user.User
import org.btcmap.settings.authToken
import org.btcmap.settings.prefs
import org.btcmap.util.rethrowIfCancellation

fun Fragment.showAuthDialog(onSuccess: () -> Unit) {
    showAccountChoicesDialog(onSuccess)
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
        val user = try {
            api().createUser(name = username, password = password)
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            val message = e.message?.takeIf { it.isNotBlank() }
                ?: getString(R.string.failed_to_create_new_account)
            showAuthError("Failed to create new account", message, e)
            return@launch
        }

        val signIn = try {
            api().signIn(
                username = user.name,
                password = password,
                label = "BTC Map Android ${BuildConfig.VERSION_CODE}",
            )
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            Toast.makeText(
                requireContext(),
                getString(R.string.account_created_sign_in_failed),
                Toast.LENGTH_LONG,
            ).show()
            showSignInDialog(onComplete, prefilledUsername = user.name)
            return@launch
        }

        completeSignIn(signIn, onComplete)
    }
}

private fun Fragment.showSignInDialog(
    onComplete: () -> Unit,
    prefilledUsername: String? = null,
) {
    val dialogView = layoutInflater.inflate(R.layout.account_dialog, null)
    val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
    val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.passwordInput)

    prefilledUsername?.let { usernameInput.setText(it) }

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
        val signIn = try {
            api().signIn(
                username,
                password,
                "BTC Map Android ${BuildConfig.VERSION_CODE}"
            )
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            val message = e.message?.takeIf { it.isNotBlank() }
                ?: getString(R.string.failed_to_sign_in)
            showAuthError("Sign in failed", message, e)
            return@launch
        }

        completeSignIn(signIn, onComplete)
    }
}

private suspend fun Fragment.completeSignIn(response: CreateTokenResponse, onComplete: () -> Unit) {
    try {
        withContext(Dispatchers.IO) {
            db().user.insert(
                User(
                    id = response.user.id,
                    name = response.user.name,
                    roles = response.user.roles,
                    savedPlaces = response.user.savedPlaces,
                    savedAreas = response.user.savedAreas,
                )
            )
            prefs.authToken = response.token
        }
        Toast.makeText(
            requireContext(),
            getString(R.string.logged_in_as, response.user.name),
            Toast.LENGTH_SHORT,
        ).show()
        onComplete()
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        val message = e.message?.takeIf { it.isNotBlank() }
            ?: getString(R.string.failed_to_sign_in)
        showAuthError("Failed to store signed-in account", message, e)
    }
}

private fun Fragment.showAuthError(logMessage: String, message: String, e: Throwable) {
    Log.e("auth", logMessage, e)
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.error)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show()
}
