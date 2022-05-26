package element

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import db.Element
import org.btcmap.R
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.databinding.FragmentElementBinding

class ElementFragment : Fragment() {

    private var _binding: FragmentElementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setScrollProgress(progress: Float) {
        binding.divider.alpha = 0.12f * progress
    }

    fun setElement(element: Element) {
        binding.toolbar.title = element.tags["name"]?.jsonPrimitive?.content ?: "Unnamed"
        binding.phone.text = element.tags["phone"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)
        binding.website.text = element.tags["website"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)
        binding.openingHours.text = element.tags["opening_hours"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)
        binding.tags.text = element.tags.toString()
    }
}