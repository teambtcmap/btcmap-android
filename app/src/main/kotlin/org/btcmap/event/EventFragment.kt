package org.btcmap.event

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import org.btcmap.util.toUrlOrNull
import org.btcmap.R
import org.btcmap.api.GetEventsItem
import org.btcmap.databinding.EventFragmentBinding
import org.btcmap.db.table.event.Event
import org.btcmap.map.toEventGeoJson
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.boostedMarkerIconColor
import org.btcmap.settings.mapStyle
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.markerIconColor
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.btcmap.ui.EventScreenLabels
import java.time.ZonedDateTime

private const val ARG_ID = "id"
private const val ARG_LAT = "lat"
private const val ARG_LON = "lon"
private const val ARG_NAME = "name"
private const val ARG_WEBSITE = "website"
private const val ARG_STARTS_AT = "starts_at"
private const val ARG_ENDS_AT = "ends_at"

fun Event.toBundle(): Bundle = Bundle().apply {
    putLong(ARG_ID, id)
    putDouble(ARG_LAT, lat)
    putDouble(ARG_LON, lon)
    putString(ARG_NAME, name)
    putString(ARG_WEBSITE, website?.toString())
    putString(ARG_STARTS_AT, startsAt.toString())
    putString(ARG_ENDS_AT, endsAt?.toString())
}

fun GetEventsItem.toBundle(): Bundle = Bundle().apply {
    putLong(ARG_ID, id)
    putDouble(ARG_LAT, lat)
    putDouble(ARG_LON, lon)
    putString(ARG_NAME, name)
    putString(ARG_WEBSITE, website?.toString())
    putString(ARG_STARTS_AT, startsAt.toString())
    putString(ARG_ENDS_AT, endsAt?.toString())
}

/**
 * An event's screen: the toolbar with the directions action over the shared
 * [org.btcmap.ui.EventScreen], which renders the event map, dates and website.
 */
class EventFragment : Fragment() {

    private val event by lazy {
        val args = requireArguments()
        Event(
            id = args.getLong(ARG_ID),
            lat = args.getDouble(ARG_LAT),
            lon = args.getDouble(ARG_LON),
            name = args.getString(ARG_NAME).orEmpty(),
            website = args.getString(ARG_WEBSITE)?.toUrlOrNull(),
            startsAt = ZonedDateTime.parse(requireNotNull(args.getString(ARG_STARTS_AT))),
            endsAt = args.getString(ARG_ENDS_AT)?.let { ZonedDateTime.parse(it) },
        )
    }

    private var _binding: EventFragmentBinding? = null
    private val binding get() = _binding!!

    val eventId: Long get() = event.id

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = EventFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.directions -> {
                    openDirections()
                    true
                }

                else -> false
            }
        }
        binding.toolbar.title = event.name

        renderEvent()
    }

    private fun renderEvent() {
        binding.eventContent.apply {
            event = this@EventFragment.event
            geoJson = listOf(this@EventFragment.event).toEventGeoJson()
            styleUrl = prefs.mapStyle.uri(requireContext())
            labels = EventScreenLabels(
                dateRange = { date, start, end ->
                    getString(R.string.event_date_time_range, date, start, end)
                },
            )
            markerBackgroundColor = Color(prefs.markerBackgroundColor(requireContext()))
            markerIconColor = Color(prefs.markerIconColor(requireContext()))
            boostedMarkerBackgroundColor = Color(prefs.boostedMarkerBackgroundColor())
            boostedMarkerIconColor = Color(prefs.boostedMarkerIconColor())
            markerBadgeBackgroundColor = Color(prefs.badgeBackgroundColor(requireContext()))
            markerBadgeTextColor = Color(prefs.badgeTextColor(requireContext()))
            usingOpenFreeMap = true
            iconTypeface = org.btcmap.util.iconTypeface
        }
    }

    private fun openDirections() {
        val coordinates = "${event.lat},${event.lon}"
        val uri = "geo:$coordinates?q=$coordinates".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(Intent.createChooser(intent, null))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
