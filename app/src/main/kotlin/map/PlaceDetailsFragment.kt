package map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.databinding.FragmentPlaceDetailsBinding
import com.google.gson.GsonBuilder
import db.Place

class PlaceDetailsFragment : Fragment() {

    private var _binding: FragmentPlaceDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setScrollProgress(progress: Float) {
        binding.divider.alpha = 0.12f * progress
    }

    fun setPlace(place: Place) {
        if (place.tags.has("name")) {
            binding.toolbar.title = place.tags["name"].asString
        } else {
            binding.toolbar.title = place.id.toString()
        }

        if (place.tags.has("phone")) {
            binding.phone.text = place.tags["phone"].asString
        } else {
            binding.phone.setText(R.string.not_provided)
        }

        if (place.tags.has("website")) {
            binding.website.text = place.tags["website"].asString
        } else {
            binding.website.setText(R.string.not_provided)
        }

        if (place.tags.has("opening_hours")) {
            binding.openingHours.text = place.tags["opening_hours"].asString
        } else {
            binding.openingHours.setText(R.string.not_provided)
        }

        binding.tags.text = GsonBuilder().setPrettyPrinting().create().toJson(place.tags)
    }
}