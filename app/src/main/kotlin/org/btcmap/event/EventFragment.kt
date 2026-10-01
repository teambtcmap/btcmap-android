package org.btcmap.event

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.R
import org.btcmap.api.GetEventsItem
import org.btcmap.databinding.EventFragmentBinding
import org.btcmap.db.table.event.Event
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.boostedMarkerIconColor
import org.btcmap.settings.mapStyle
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.markerIconColor
import androidx.compose.ui.graphics.Color
import org.btcmap.map.toEventGeoJson
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

class EventFragment : Fragment() {

    private val event by lazy {
        val args = requireArguments()
        Event(
            id = args.getLong(ARG_ID),
            lat = args.getDouble(ARG_LAT),
            lon = args.getDouble(ARG_LON),
            name = args.getString(ARG_NAME).orEmpty(),
            website = args.getString(ARG_WEBSITE)?.toHttpUrlOrNull(),
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

        binding.zoomIn.setOnClickListener {
            binding.map.zoomIn()
        }

        binding.zoomOut.setOnClickListener {
            binding.map.zoomOut()
        }

        renderEvent()
        renderEventMap()
    }

    private fun renderEvent() {
        binding.name.text = event.name

        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
            FormatStyle.MEDIUM,
            FormatStyle.SHORT,
        )

        val start = event.startsAt
        val end = event.endsAt

        when {
            end == null -> {
                binding.startDate.text = start.format(dateTimeFormatter)
                binding.endDate.isVisible = false
            }

            start.toLocalDate() == end.toLocalDate() -> {
                val date = start.format(dateFormatter)
                val startTime = start.format(timeFormatter)
                val endTime = end.format(timeFormatter)
                binding.startDate.text = getString(
                    R.string.event_date_time_range,
                    date,
                    startTime,
                    endTime,
                )
                binding.endDate.isVisible = false
            }

            else -> {
                binding.startDate.text = start.format(dateTimeFormatter)
                binding.endDate.text = end.format(dateTimeFormatter)
                binding.endDate.isVisible = true
            }
        }

        binding.website.isVisible = event.website != null
        binding.website.text = event.website?.toString()
    }

    private fun openDirections() {
        val coordinates = "${event.lat},${event.lon}"
        val uri = "geo:$coordinates?q=$coordinates".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(Intent.createChooser(intent, null))
    }

    /** Points the shared event map at the event, with its marker. */
    private fun renderEventMap() {
        binding.map.apply {
            lat = event.lat
            lon = event.lon
            geoJson = listOf(event).toEventGeoJson()
            styleUrl = prefs.mapStyle.uri(requireContext())
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
    }
}
