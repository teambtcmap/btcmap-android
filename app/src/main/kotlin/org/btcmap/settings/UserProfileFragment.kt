package org.btcmap.settings

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
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.getUser
import org.btcmap.api.removeSavedArea
import org.btcmap.api.removeSavedPlace
import org.btcmap.api.updatePassword
import org.btcmap.api.updateUsername
import org.btcmap.db.table.user.SavedItem
import org.btcmap.db.table.user.User
import org.btcmap.databinding.SavedAreaItemBinding
import org.btcmap.databinding.SavedPlaceItemBinding
import org.btcmap.databinding.UserProfileFragmentBinding
import org.btcmap.db
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError

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

        val user = db().user.select()!!
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

        binding.logoutButton.setOnClickListener {
            logout()
        }

        binding.changeUsernameButton.setOnClickListener {
            showChangeUsernameDialog()
        }

        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.change_password_dialog, null)
        val currentInput = dialogView.findViewById<TextInputEditText>(R.id.currentPasswordInput)
        val newInput = dialogView.findViewById<TextInputEditText>(R.id.newPasswordInput)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_password)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val current = currentInput.text.toString()
                val new = newInput.text.toString()
                if (current.isNotEmpty() && new.isNotEmpty()) {
                    changePassword(current, new)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api().updatePassword(oldPassword, newPassword)
                Toast.makeText(context, R.string.password_changed, Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error)
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private fun showChangeUsernameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.change_username_dialog, null)
        val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.usernameInput)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_username)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = usernameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    changeUsername(newName)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun changeUsername(newName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = api().updateUsername(newName)
                val existing = db().user.select()
                db().user.delete()
                db().user.insert(
                    User(
                        id = user.id,
                        name = user.name,
                        roles = user.roles,
                        savedPlaces = existing?.savedPlaces ?: user.savedPlaces,
                        savedAreas = existing?.savedAreas ?: user.savedAreas,
                    )
                )
                binding.username.text = user.name
                Toast.makeText(context, R.string.username_changed, Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error)
                    .setMessage(e.message)
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                prefs.authToken = null
                db().user.delete()
                parentFragmentManager.popBackStack()
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
                db().user.delete()
                db().user.insert(
                    User(
                        id = user.id,
                        name = user.name,
                        roles = user.roles,
                        savedPlaces = user.savedPlaces,
                        savedAreas = user.savedAreas,
                    )
                )
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
