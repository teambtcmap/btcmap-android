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
import com.google.android.material.textview.MaterialTextView
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import org.btcmap.R
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
        binding.savedPlacesList.adapter = SavedPlacesAdapter(user.savedPlaces)

        binding.noSavedPlaces.isVisible = user.savedPlaces.size() == 0
        binding.savedPlacesList.isVisible = user.savedPlaces.size() > 0

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
                Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class SavedPlacesAdapter(
        private val places: JsonArray,
    ) : RecyclerView.Adapter<SavedPlacesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: MaterialTextView = view as MaterialTextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val text = MaterialTextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(48, 32, 48, 32)
            }
            return ViewHolder(text)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = places[position].asJsonObject["name"].asString
        }

        override fun getItemCount(): Int = places.size()
    }
}
