package map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.databinding.FragmentPlaceDetailsBinding
import db.Place
import kotlinx.serialization.json.jsonPrimitive

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
        binding.toolbar.title = place.tags["name"]?.jsonPrimitive?.content ?: place.id
        binding.phone.text = place.tags["phone"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)
        binding.website.text = place.tags["website"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)
        binding.openingHours.text = place.tags["opening_hours"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)
        binding.tags.text = place.tags.toString()
    }
}