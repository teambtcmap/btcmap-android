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
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.db.table.User
import org.btcmap.databinding.SavedAreaItemBinding
import org.btcmap.databinding.SavedPlaceItemBinding
import org.btcmap.databinding.UserProfileFragmentBinding
import org.btcmap.db

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
        binding.username.text = getString(R.string.logged_in_as, user.name)

        binding.savedPlacesList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedPlacesList.adapter = SavedPlacesAdapter(
            places = user.savedPlaces,
            onDeleteClick = { placeId ->
                deleteSavedPlace(placeId)
            }
        )

        binding.noSavedPlaces.isVisible = user.savedPlaces.size() == 0
        binding.savedPlacesList.isVisible = user.savedPlaces.size() > 0

        binding.savedAreasList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedAreasList.adapter = SavedAreasAdapter(
            areas = user.savedAreas,
            onDeleteClick = { areaId ->
                deleteSavedArea(areaId)
            }
        )

        binding.noSavedAreas.isVisible = user.savedAreas.size() == 0
        binding.savedAreasList.isVisible = user.savedAreas.size() > 0

        binding.logoutButton.setOnClickListener {
            logout()
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
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteSavedPlace(placeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api().removeSavedPlace(placeId)
                refreshUserData()
            } catch (e: Throwable) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteSavedArea(areaId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api().removeSavedArea(areaId)
                refreshUserData()
            } catch (e: Throwable) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
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
                binding.noSavedPlaces.isVisible = user.savedPlaces.size() == 0
                binding.savedPlacesList.isVisible = user.savedPlaces.size() > 0

                binding.savedAreasList.adapter = SavedAreasAdapter(
                    areas = user.savedAreas,
                    onDeleteClick = { areaId ->
                        deleteSavedArea(areaId)
                    }
                )
                binding.noSavedAreas.isVisible = user.savedAreas.size() == 0
                binding.savedAreasList.isVisible = user.savedAreas.size() > 0
            } catch (e: Throwable) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class SavedPlacesAdapter(
        private val places: JsonArray,
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
            val place = places[position].asJsonObject
            holder.binding.placeName.text = place["name"].asString
            val placeId = place["id"].asLong
            holder.binding.deleteButton.setOnClickListener {
                onDeleteClick(placeId)
            }
        }

        override fun getItemCount(): Int = places.size()
    }

    private class SavedAreasAdapter(
        private val areas: JsonArray,
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
            val area = areas[position].asJsonObject
            holder.binding.areaName.text = area["name"].asString
            val areaId = area["id"].asLong
            holder.binding.deleteButton.setOnClickListener {
                onDeleteClick(areaId)
            }
        }

        override fun getItemCount(): Int = areas.size()
    }
}