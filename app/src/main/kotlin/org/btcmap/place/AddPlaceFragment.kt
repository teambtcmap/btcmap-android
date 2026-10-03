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

/**
 * The add-place screen: a toolbar over the shared
 * [org.btcmap.ui.AddPlaceScreen], which owns the positioning map, the form, the
 * validation and the submit. This fragment only supplies the labels, the target
 * position and the submit call.
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

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.addPlaceContent.apply {
            lat = args.lat
            lon = args.lon
            styleUrl = prefs.mapStyle.uri(requireContext())
            labels = AddPlaceLabels(
                name = getString(R.string.name),
                category = getString(R.string.category),
                address = getString(R.string.address),
                website = getString(R.string.website_optional),
                description = getString(R.string.description_optional),
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
