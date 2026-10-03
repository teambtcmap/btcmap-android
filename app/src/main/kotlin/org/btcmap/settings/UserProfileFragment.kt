package org.btcmap.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.account.AccountSession
import org.btcmap.api
import org.btcmap.app
import org.btcmap.auth.registerChangePasswordResultListener
import org.btcmap.auth.showChangePasswordDialog
import org.btcmap.db
import org.btcmap.db.table.user.User
import org.btcmap.databinding.UserProfileFragmentBinding
import org.btcmap.saved.removeSavedArea
import org.btcmap.saved.removeSavedPlace
import org.btcmap.saved.withLocalizedAreaNames
import org.btcmap.saved.withLocalizedPlaceNames
import org.btcmap.ui.SavedItemUi
import org.btcmap.ui.UserProfileLabels
import org.btcmap.ui.UserProfileUiState
import org.btcmap.util.iconTypeface
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.setFieldError
import org.btcmap.util.showError
import org.btcmap.util.userFacingMessage

class UserProfileFragment : Fragment() {

    private var _binding: UserProfileFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = UserProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.userProfileList.iconTypeface = iconTypeface
        binding.userProfileList.onEditUsername = { showChangeUsernameDialog() }
        binding.userProfileList.onEditPassword = { showChangePasswordDialog() }
        binding.userProfileList.onDeletePlace = { deleteSavedPlace(it) }
        binding.userProfileList.onDeleteArea = { deleteSavedArea(it) }
        binding.userProfileList.onLogOut = { logout() }

        registerChangePasswordResultListener {
            Toast.makeText(context, R.string.password_changed, Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            loadUser()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Reads the cached account and renders it, signing out if there is none. */
    private suspend fun loadUser() {
        val user = withContext(Dispatchers.IO) { db().user.select() }
        if (user == null) {
            // With no cached account there is no usable session, so clear any
            // leftover stored token instead of leaving it behind.
            withContext(Dispatchers.IO) {
                prefs.clearSession(db())
            }
            parentFragmentManager.popBackStack()
            return
        }
        renderUser(user)
    }

    private suspend fun renderUser(user: User) {
        val labels = UserProfileLabels(
            username = getString(R.string.username),
            password = getString(R.string.password),
            savedPlaces = getString(R.string.saved_places),
            savedAreas = getString(R.string.saved_areas),
            noSavedPlaces = getString(R.string.no_saved_places),
            noSavedAreas = getString(R.string.no_saved_areas),
            logOut = getString(R.string.logout),
            editUsername = getString(R.string.change_username),
            editPassword = getString(R.string.change_password),
            delete = getString(R.string.delete),
        )

        val state = UserProfileUiState(
            username = user.name,
            password = getString(R.string.password_mask),
            savedPlaces = user.savedPlaces
                .withLocalizedPlaceNames(db())
                .map { SavedItemUi(it.id, it.name) },
            savedAreas = user.savedAreas
                .withLocalizedAreaNames(db())
                .map { SavedItemUi(it.id, it.name) },
            labels = labels,
        )

        _binding?.userProfileList?.state = state
    }

    private fun showChangeUsernameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.change_username_dialog, null)
        val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_username)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val newName = usernameInput.text.toString().trim()
                if (newName.isEmpty()) {
                    usernameInput.setFieldError(getString(R.string.field_required))
                    return@setOnClickListener
                }

                dialog.dismiss()
                changeUsername(newName)
            }
        }

        dialog.show()
    }

    private fun changeUsername(newName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AccountSession.changeUsername(api(), db(), newName)
                loadUser()
                Toast.makeText(context, R.string.username_changed, Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error)
                    .setMessage(e.userFacingMessage(getString(R.string.error)))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private fun logout() {
        val application = app()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Clear only the session this screen saw: a sign-in that raced
                // this logout committed a new token, which must not be dropped.
                val cleared = AccountSession.clearSession(db(), prefs)
                parentFragmentManager.popBackStack()

                // Revoke the token server-side, but only the one that was just
                // cleared. This is best-effort and runs on the app scope so it
                // also completes if it outlives this screen.
                cleared?.let { application.revokeToken(it) }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun deleteSavedPlace(placeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                renderUser(removeSavedPlace(placeId))
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun deleteSavedArea(areaId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                renderUser(removeSavedArea(areaId))
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }
}
