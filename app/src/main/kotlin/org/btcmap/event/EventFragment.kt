package org.btcmap.event

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.R
import org.btcmap.api.GetEventsItem
import org.btcmap.databinding.EventFragmentBinding
import org.btcmap.db.table.event.Event
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.btcmap.map.ICON_OFFSET_Y
import org.btcmap.map.ensureEventMarkerImage
import org.btcmap.settings.mapStyle
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val ARG_ID = "id"
private const val ARG_AREA_ID = "area_id"
private const val ARG_LAT = "lat"
private const val ARG_LON = "lon"
private const val ARG_NAME = "name"
private const val ARG_WEBSITE = "website"
private const val ARG_STARTS_AT = "starts_at"
private const val ARG_ENDS_AT = "ends_at"

fun Event.toBundle(): Bundle = Bundle().apply {
    putLong(ARG_ID, id)
    putString(ARG_AREA_ID, areaId?.toString())
    putDouble(ARG_LAT, lat)
    putDouble(ARG_LON, lon)
    putString(ARG_NAME, name)
    putString(ARG_WEBSITE, website?.toString())
    putString(ARG_STARTS_AT, startsAt.toString())
    putString(ARG_ENDS_AT, endsAt?.toString())
}

fun GetEventsItem.toBundle(): Bundle = Bundle().apply {
    putLong(ARG_ID, id)
    putString(ARG_AREA_ID, areaId?.toString())
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
            areaId = args.getString(ARG_AREA_ID)?.toLongOrNull(),
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

    private var map: MapLibreMap? = null

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
            map?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.zoomOut.setOnClickListener {
            map?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        renderEvent()
        initMap()
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

    private fun initMap() {
        binding.map.getMapAsync { map ->
            this.map = map
            map.setStyle(Style.Builder().fromUri(prefs.mapStyle.uri(requireContext())))
            map.uiSettings.setAllGesturesEnabled(true)
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isCompassEnabled = false

            map.getStyle { style ->
                if (style.getImage("btcmap-marker") == null) {
                    val drawable = AppCompatResources
                        .getDrawable(requireContext(), R.drawable.map_marker)!!
                        .mutate()
                    DrawableCompat.setTint(drawable, prefs.markerBackgroundColor(requireContext()))
                    style.addImage("btcmap-marker", drawable)
                }
                ensureEventMarkerImage(requireContext(), style)
                renderEventMarker(style)
            }

            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(event.lat, event.lon),
                    EVENT_ZOOM,
                )
            )
        }
    }

    private fun renderEventMarker(style: Style) {
        if (style.getSource(MARKER_SOURCE_ID) != null) return

        style.addSource(
            GeoJsonSource(MARKER_SOURCE_ID, Point.fromLngLat(event.lon, event.lat))
        )

        style.addLayer(
            SymbolLayer(MARKER_PIN_LAYER_ID, MARKER_SOURCE_ID).withProperties(
                PropertyFactory.iconImage("btcmap-marker"),
                PropertyFactory.iconAnchor(ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
            )
        )

        style.addLayer(
            SymbolLayer(MARKER_ICON_LAYER_ID, MARKER_SOURCE_ID).withProperties(
                PropertyFactory.iconImage(EVENT_MARKER_ICON_NAME),
                PropertyFactory.iconAnchor(ICON_ANCHOR_CENTER),
                PropertyFactory.iconOffset(arrayOf(0f, ICON_OFFSET_Y)),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        map = null
    }

    companion object {
        private const val EVENT_ZOOM = 15.0

        private const val MARKER_SOURCE_ID = "event_marker_source"
        private const val MARKER_PIN_LAYER_ID = "event_marker_pin"
        private const val MARKER_ICON_LAYER_ID = "event_marker_icon"
    }
}
