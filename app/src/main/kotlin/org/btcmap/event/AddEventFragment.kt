package org.btcmap.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.submitEvent
import org.btcmap.databinding.AddEventFragmentBinding
import org.btcmap.settings.mapStyle
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.btcmap.ui.AddEventLabels
import org.btcmap.ui.markerPalette
import java.time.LocalDateTime

/**
 * The add-event screen: the shared [org.btcmap.ui.AddEventScreen] and nothing
 * else. The screen owns its top bar, the positioning map, the form, the
 * validation and the submit; this fragment only supplies the labels, the target
 * position, the icon typeface, the marker colours and the submit call.
 */
class AddEventFragment : Fragment() {

    private data class Args(
        val lat: Double,
        val lon: Double,
        val name: String,
        val website: String,
        val startsAt: LocalDateTime?,
        val endsAt: LocalDateTime?,
    )

    private val args by lazy {
        Args(
            lat = requireArguments().getDouble("lat"),
            lon = requireArguments().getDouble("lon"),
            name = requireArguments().getString("name").orEmpty(),
            website = requireArguments().getString("website").orEmpty(),
            startsAt = requireArguments().getString("starts_at")?.toLocalDateTimeOrNull(),
            endsAt = requireArguments().getString("ends_at")?.toLocalDateTimeOrNull(),
        )
    }

    private var _binding: AddEventFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AddEventFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addEventContent.apply {
            lat = args.lat
            lon = args.lon
            initialName = args.name
            initialWebsite = args.website
            initialStartsAt = args.startsAt
            initialEndsAt = args.endsAt
            styleUrl = prefs.mapStyle.uri(requireContext())
            iconTypeface = org.btcmap.util.iconTypeface
            palette = markerPalette(prefs)
            labels = AddEventLabels(
                title = getString(R.string.add_event_title),
                back = getString(R.string.navigate_up),
                name = getString(R.string.name),
                namePlaceholder = getString(R.string.event_name_placeholder),
                website = getString(R.string.event_website),
                websitePlaceholder = getString(R.string.website_placeholder),
                startsAt = getString(R.string.event_starts_at),
                endsAt = getString(R.string.event_ends_at),
                selectDateTime = getString(R.string.event_select_date_time),
                clearEnd = getString(R.string.event_clear_end),
                dragMap = getString(R.string.add_location_drag_to_adjust),
                required = getString(R.string.field_required),
                submit = getString(R.string.submit_event),
                submitted = getString(R.string.event_submitted),
                backToMap = getString(R.string.back_to_map),
                ok = getString(R.string.ok),
                cancel = getString(R.string.cancel),
            )
            submit = { draft ->
                api().submitEvent(
                    lat = draft.lat,
                    lon = draft.lon,
                    name = draft.name,
                    website = draft.website,
                    startsAt = draft.startsAt,
                    endsAt = draft.endsAt,
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

/** Parses the floating local date-time a duplicate pre-fill carries, or null. */
private fun String.toLocalDateTimeOrNull(): LocalDateTime? =
    runCatching { LocalDateTime.parse(this) }.getOrNull()
