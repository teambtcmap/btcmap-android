package org.btcmap.auth

import android.util.Log
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
import java.util.UUID

fun Fragment.showAuthDialog(message: String, onSuccess: () -> Unit) {
    if (message.isBlank()) {
        val options =
            arrayOf(
                getString(R.string.i_don_t_have_an_account),
                getString(R.string.log_in_with_existing_account)
            )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createNewAccount(onSuccess)
                    1 -> showSignInDialog(onSuccess)
                }
            }
            .show()
    } else {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account)
            .setMessage(message)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                val options =
                    arrayOf(
                        getString(R.string.i_don_t_have_an_account),
                        getString(R.string.log_in_with_existing_account)
                    )
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.account)
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> createNewAccount(onSuccess)
                            1 -> showSignInDialog(onSuccess)
                        }
                    }
                    .show()
            }
            .show()
    }
}

private fun Fragment.createNewAccount(onComplete: () -> Unit) {
    val password = UUID.randomUUID().toString()

    viewLifecycleOwner.lifecycleScope.launch {
        try {
            val user = api().createUser(password)
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
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account)
                .setMessage(
                    getString(
                        R.string.logged_in_as_you_can_visit_settings_to_customize_and_backup_your_account,
                        user.name,
                    )
                )
                .show()
            onComplete()
        } catch (e: Exception) {
            val message = getString(R.string.failed_to_create_new_account)
            Log.e("auth", message, e)
            Toast.makeText(
                requireContext(),
                "$message: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

private fun Fragment.showSignInDialog(onComplete: () -> Unit) {
    val dialogView = layoutInflater.inflate(R.layout.login_dialog, null)
    val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
    val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.passwordInput)
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.login)
        .setView(dialogView)
        .setPositiveButton(R.string.login) { _, _ ->
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                signIn(username, password, onComplete)
            }
        }
        .setNegativeButton(R.string.btn_continue, null)
        .show()
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
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }
}