package org.btcmap.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.toDbUser
import org.btcmap.api.updateUsername
import org.btcmap.app
import org.btcmap.auth.registerChangePasswordResultListener
import org.btcmap.auth.showChangePasswordDialog
import org.btcmap.db
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.btcmap.databinding.SavedItemBinding
import org.btcmap.databinding.UserProfileFragmentBinding
import org.btcmap.i18n.getLocalizedName
import org.btcmap.saved.removeSavedArea
import org.btcmap.saved.removeSavedPlace
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

        binding.logoutButton.setOnClickListener {
            logout()
        }

        binding.changeUsernameButton.setOnClickListener {
            showChangeUsernameDialog()
        }

        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        registerChangePasswordResultListener {
            Toast.makeText(context, R.string.password_changed, Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { db().user.select() }
            if (user == null) {
                // With no cached account there is no usable session, so clear any
                // leftover stored token instead of leaving it behind.
                withContext(Dispatchers.IO) {
                    prefs.clearSession(db())
                }
                parentFragmentManager.popBackStack()
                return@launch
            }
            bindUser(user)
        }
    }

    private suspend fun bindUser(user: User) {
        binding.username.text = user.name
        binding.password.text = getString(R.string.password_mask)
        renderSavedItems(user)
    }

    private suspend fun renderSavedItems(user: User) {
        binding.savedPlacesList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedPlacesList.adapter = SavedItemsAdapter(
            items = user.savedPlaces.withLocalizedPlaceNames(),
            onDeleteClick = { placeId ->
                deleteSavedPlace(placeId)
            }
        )

        binding.noSavedPlaces.isVisible = user.savedPlaces.isEmpty()
        binding.savedPlacesList.isVisible = user.savedPlaces.isNotEmpty()

        binding.savedAreasList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedAreasList.adapter = SavedItemsAdapter(
            items = user.savedAreas.withLocalizedAreaNames(),
            onDeleteClick = { areaId ->
                deleteSavedArea(areaId)
            }
        )

        binding.noSavedAreas.isVisible = user.savedAreas.isEmpty()
        binding.savedAreasList.isVisible = user.savedAreas.isNotEmpty()
    }

    /**
     * The user endpoint returns each saved entity's base name, with no `lang`,
     * so the names are re-resolved against the local cache when it holds the
     * entity. A name that is missing locally falls back to the server's.
     */
    private suspend fun List<SavedItem>.withLocalizedAreaNames(): List<SavedItem> =
        mapSavedNames { db().area.selectById(it.id)?.getLocalizedName() }

    private suspend fun List<SavedItem>.withLocalizedPlaceNames(): List<SavedItem> =
        mapSavedNames { db().place.selectById(it.id)?.getLocalizedName() }

    private suspend fun List<SavedItem>.mapSavedNames(
        localizedName: suspend (SavedItem) -> String?,
    ): List<SavedItem> = withContext(Dispatchers.IO) {
        this@mapSavedNames.map { item ->
            localizedName(item)
                ?.takeIf { it.isNotBlank() }
                ?.let { item.copy(name = it) }
                ?: item
        }
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
                val user = api().updateUsername(newName)
                val updated = user.toDbUser()
                withContext(Dispatchers.IO) {
                    val database = db()
                    val existing = database.user.select()
                    database.transaction {
                        database.user.delete()
                        database.user.insert(
                            updated.copy(
                                // The username endpoint returns the saved lists
                                // empty, so keep the cached ones.
                                savedPlaces = existing?.savedPlaces ?: updated.savedPlaces,
                                savedAreas = existing?.savedAreas ?: updated.savedAreas,
                            )
                        )
                    }
                }
                binding.username.text = user.name
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun logout() {
        val application = app()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { prefs.authToken }
                // Clear only the session this screen saw: a sign-in that raced
                // this logout committed a new token, which must not be dropped.
                val cleared = withContext(Dispatchers.IO) {
                    val stored = token?.takeIf { it.isNotBlank() }
                    if (stored == null) {
                        prefs.clearSession(db())
                        false
                    } else {
                        prefs.clearSessionIfTokenMatches(db(), stored)
                    }
                }
                parentFragmentManager.popBackStack()

                // Revoke the token server-side, but only the one that was just
                // cleared. This is best-effort and runs on the app scope so it
                // also completes if it outlives this screen.
                if (cleared) {
                    token?.let { application.revokeToken(it) }
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun deleteSavedPlace(placeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                renderSavedItems(removeSavedPlace(placeId))
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun deleteSavedArea(areaId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                renderSavedItems(removeSavedArea(areaId))
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    /**
     * Both saved lists render the same row, so one adapter serves places and
     * areas and only the data and delete callback differ.
     */
    private class SavedItemsAdapter(
        private val items: List<SavedItem>,
        private val onDeleteClick: (Long) -> Unit,
    ) : RecyclerView.Adapter<SavedItemsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding: SavedItemBinding = SavedItemBinding.bind(view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = SavedItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return ViewHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.name.text = item.name
            holder.binding.deleteButton.setOnClickListener {
                onDeleteClick(item.id)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
