package map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.databinding.FragmentPlaceDetailsBinding
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
        if ("name" in place.tags) {
            binding.toolbar.title = place.tags["name"]
        } else {
            binding.toolbar.title = place.id
        }

        if ("phone" in place.tags) {
            binding.phone.text = place.tags["phone"]
        } else {
            binding.phone.setText(R.string.not_provided)
        }

        if ("website" in place.tags) {
            binding.website.text = place.tags["website"]
        } else {
            binding.website.setText(R.string.not_provided)
        }

        if ("opening_hours" in place.tags) {
            binding.openingHours.text = place.tags["opening_hours"]
        } else {
            binding.openingHours.setText(R.string.not_provided)
        }

        binding.tags.text = place.tags.toFormattedJson()
    }
}