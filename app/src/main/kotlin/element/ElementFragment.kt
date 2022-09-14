package element

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import db.Element
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.R
import org.btcmap.databinding.FragmentElementBinding

class ElementFragment : Fragment() {

    private var _binding: FragmentElementBinding? = null
    private val binding get() = _binding!!

    private val tagsJsonFormatter by lazy { Json { prettyPrint = true } }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_edit,
                R.id.action_delete -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://wiki.openstreetmap.org/wiki/How_to_contribute")
                    startActivity(intent)
                }
            }

            true
        }

        binding.tagsButton.setOnClickListener {
            binding.tags.isVisible = !binding.tags.isVisible

            if (binding.tags.isVisible) {
                binding.tagsButton.setText(R.string.hide_tags)
            } else {
                binding.tagsButton.setText(R.string.show_tags)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setScrollProgress(progress: Float) {
        if (progress == 1.0f) {
            binding.toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        } else {
            binding.toolbar.navigationIcon = null
        }

        val edit = binding.toolbar.menu.findItem(R.id.action_edit)!!
        edit.isVisible = progress == 1.0f

        val delete = binding.toolbar.menu.findItem(R.id.action_delete)!!
        delete.isVisible = progress == 1.0f

        binding.divider.alpha = 0.12f * progress
    }

    fun setElement(element: Element) {
        binding.toolbar.title = element.tags["name"]?.jsonPrimitive?.content ?: "Unnamed"

        val address = buildString {
            if (element.tags.containsKey("addr:housenumber")) {
                append(element.tags["addr:housenumber"]!!.jsonPrimitive.content)
            }

            if (element.tags.containsKey("addr:street")) {
                append(" ")
                append(element.tags["addr:street"]!!.jsonPrimitive.content)
            }

            if (element.tags.containsKey("addr:city")) {
                append(", ")
                append(element.tags["addr:city"]!!.jsonPrimitive.content)
            }

            if (element.tags.containsKey("addr:postcode")) {
                append(", ")
                append(element.tags["addr:postcode"]!!.jsonPrimitive.content)
            }
        }.trim(',', ' ')

        binding.address.isVisible = address.isNotBlank()
        binding.address.text = address

        binding.phone.text = element.tags["phone"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)
        binding.website.text = element.tags["website"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)
        binding.openingHours.text =
            element.tags["opening_hours"]?.jsonPrimitive?.content ?: getString(R.string.not_provided)

        binding.tags.text = tagsJsonFormatter.encodeToString(JsonObject.serializer(), element.tags)
    }
}