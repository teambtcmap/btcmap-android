package org.btcmap.place

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.submitPlace
import org.btcmap.databinding.AddPlaceFragmentBinding
import org.btcmap.settings.mapStyle
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.btcmap.ui.AddPlaceLabels
import org.btcmap.ui.markerPalette

/**
 * The add-place screen: the shared [org.btcmap.ui.AddPlaceScreen] and nothing
 * else. The screen owns its top bar, the positioning map, the form, the
 * validation and the submit; this fragment only supplies the labels, the target
 * position, the icon typeface, the marker colours and the submit call.
 */
class AddPlaceFragment : Fragment() {

    private data class Args(
        val lat: Double,
        val lon: Double,
    )

    private val args by lazy {
        Args(
            lat = requireArguments().getDouble("lat"),
            lon = requireArguments().getDouble("lon"),
        )
    }

    private var _binding: AddPlaceFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AddPlaceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addPlaceContent.apply {
            lat = args.lat
            lon = args.lon
            styleUrl = prefs.mapStyle.uri(requireContext())
            iconTypeface = org.btcmap.util.iconTypeface
            palette = markerPalette(prefs)
            labels = AddPlaceLabels(
                title = getString(R.string.add_place_title),
                back = getString(R.string.navigate_up),
                name = getString(R.string.name),
                namePlaceholder = getString(R.string.name_placeholder),
                category = getString(R.string.category),
                categoryPlaceholder = getString(R.string.category_placeholder),
                address = getString(R.string.address),
                addressPlaceholder = getString(R.string.address_placeholder),
                website = getString(R.string.website_optional),
                websitePlaceholder = getString(R.string.website_placeholder),
                description = getString(R.string.description_optional),
                descriptionPlaceholder = getString(R.string.description_placeholder),
                dragMap = getString(R.string.add_location_drag_to_adjust),
                required = getString(R.string.field_required),
                submit = getString(R.string.submit_place),
                submitted = getString(R.string.place_submitted),
                backToMap = getString(R.string.back_to_map),
            )
            submit = { draft ->
                api().submitPlace(
                    lat = draft.lat,
                    lon = draft.lon,
                    category = draft.category,
                    name = draft.name,
                    address = draft.address.takeIf { it.isNotEmpty() },
                    website = draft.website.takeIf { it.isNotEmpty() },
                    description = draft.description.takeIf { it.isNotEmpty() },
                )
            }
            onBack = { parentFragmentManager.popBackStack() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
