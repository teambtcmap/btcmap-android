package org.btcmap.event

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.EventReviewFragmentBinding
import org.btcmap.settings.mapStyle
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.btcmap.ui.EventReviewLabels
import org.btcmap.ui.markerPalette

/**
 * The event review queue: the shared [org.btcmap.ui.EventReviewScreen] and
 * nothing else. The screen owns its top bar, the loading, the approve/reject
 * actions and the empty/error states; this fragment only supplies the API, the
 * style, the labels, the icon typeface and the marker colours.
 */
class EventReviewFragment : Fragment() {

    private var _binding: EventReviewFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = EventReviewFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.eventReviewContent.apply {
            api = api()
            styleUrl = prefs.mapStyle.uri(requireContext())
            iconTypeface = org.btcmap.util.iconTypeface
            palette = markerPalette(prefs)
            title = getString(R.string.event_review_title)
            labels = EventReviewLabels(
                back = getString(R.string.navigate_up),
                empty = getString(R.string.event_review_empty),
                failed = getString(R.string.event_review_failed),
                retry = getString(R.string.retry),
                approve = getString(R.string.event_review_approve),
                reject = getString(R.string.event_review_reject),
                actionFailed = getString(R.string.event_review_action_failed),
                dateRange = { date, start, end ->
                    getString(R.string.event_date_time_range, date, start, end)
                },
            )
            onBack = { parentFragmentManager.popBackStack() }
            onOpenUrl = { url ->
                runCatching {
                    startActivity(
                        Intent.createChooser(Intent(Intent.ACTION_VIEW, url.toUri()), null),
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
