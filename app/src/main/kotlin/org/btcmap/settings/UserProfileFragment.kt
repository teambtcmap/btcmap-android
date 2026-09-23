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
import org.btcmap.api.getUser
import org.btcmap.api.removeSavedArea
import org.btcmap.api.removeSavedPlace
import org.btcmap.api.toDbUser
import org.btcmap.api.updatePassword
import org.btcmap.api.updateUsername
import org.btcmap.app
import org.btcmap.auth.registerChangePasswordResultListener
import org.btcmap.auth.showChangePasswordDialog
import org.btcmap.db
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.btcmap.databinding.SavedAreaItemBinding
import org.btcmap.databinding.SavedPlaceItemBinding
import org.btcmap.databinding.UserProfileFragmentBinding
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

        registerChangePasswordResultListener { current, new -> changePassword(current, new) }

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

    private fun bindUser(user: User) {
        binding.username.text = user.name
        binding.password.text = getString(R.string.password_mask)

        binding.savedPlacesList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedPlacesList.adapter = SavedPlacesAdapter(
            places = user.savedPlaces,
            onDeleteClick = { placeId ->
                deleteSavedPlace(placeId)
            }
        )

        binding.noSavedPlaces.isVisible = user.savedPlaces.isEmpty()
        binding.savedPlacesList.isVisible = user.savedPlaces.isNotEmpty()

        binding.savedAreasList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedAreasList.adapter = SavedAreasAdapter(
            areas = user.savedAreas,
            onDeleteClick = { areaId ->
                deleteSavedArea(areaId)
            }
        )

        binding.noSavedAreas.isVisible = user.savedAreas.isEmpty()
        binding.savedAreasList.isVisible = user.savedAreas.isNotEmpty()
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api().updatePassword(oldPassword, newPassword)
                Toast.makeText(context, R.string.password_changed, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error)
                    .setMessage(e.userFacingMessage(getString(R.string.error)))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
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
                withContext(Dispatchers.IO) {
                    val database = db()
                    val existing = database.user.select()
                    database.transaction {
                        database.user.delete()
                        database.user.insert(
                            user.toDbUser().copy(
                                savedPlaces = existing?.savedPlaces ?: user.savedPlaces,
                                savedAreas = existing?.savedAreas ?: user.savedAreas,
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
                api().removeSavedPlace(placeId)
                refreshUserData()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun deleteSavedArea(areaId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api().removeSavedArea(areaId)
                refreshUserData()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun refreshUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = api().getUser()
                withContext(Dispatchers.IO) {
                    val database = db()
                    database.transaction {
                        database.user.delete()
                        database.user.insert(user.toDbUser())
                    }
                }
                binding.savedPlacesList.adapter = SavedPlacesAdapter(
                    places = user.savedPlaces,
                    onDeleteClick = { placeId ->
                        deleteSavedPlace(placeId)
                    }
                )
                binding.noSavedPlaces.isVisible = user.savedPlaces.isEmpty()
                binding.savedPlacesList.isVisible = user.savedPlaces.isNotEmpty()

                binding.savedAreasList.adapter = SavedAreasAdapter(
                    areas = user.savedAreas,
                    onDeleteClick = { areaId ->
                        deleteSavedArea(areaId)
                    }
                )
                binding.noSavedAreas.isVisible = user.savedAreas.isEmpty()
                binding.savedAreasList.isVisible = user.savedAreas.isNotEmpty()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private class SavedPlacesAdapter(
        private val places: List<SavedItem>,
        private val onDeleteClick: (Long) -> Unit,
    ) : RecyclerView.Adapter<SavedPlacesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding: SavedPlaceItemBinding = SavedPlaceItemBinding.bind(view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = SavedPlaceItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return ViewHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val place = places[position]
            holder.binding.placeName.text = place.name
            holder.binding.deleteButton.setOnClickListener {
                onDeleteClick(place.id)
            }
        }

        override fun getItemCount(): Int = places.size
    }

    private class SavedAreasAdapter(
        private val areas: List<SavedItem>,
        private val onDeleteClick: (Long) -> Unit,
    ) : RecyclerView.Adapter<SavedAreasAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding: SavedAreaItemBinding = SavedAreaItemBinding.bind(view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = SavedAreaItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return ViewHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val area = areas[position]
            holder.binding.areaName.text = area.name
            holder.binding.deleteButton.setOnClickListener {
                onDeleteClick(area.id)
            }
        }

        override fun getItemCount(): Int = areas.size
    }
}
