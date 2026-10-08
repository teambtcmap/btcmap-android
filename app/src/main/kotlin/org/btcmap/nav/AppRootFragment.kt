package org.btcmap.nav

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.ui.text.font.FontFamily
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil3.SingletonImageLoader
import java.time.LocalDateTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.App
import org.btcmap.api
import org.btcmap.api.getEvent
import org.btcmap.auth.authTokenLabel
import org.btcmap.databinding.AppRootFragmentBinding
import org.btcmap.db
import org.btcmap.db.table.event.Event
import org.btcmap.event.toEvent
import org.btcmap.settings.offlineStyleFamily
import org.btcmap.settings.offlineStyleUrl
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.btcmap.ui.AppRoute
import org.btcmap.ui.AppServices
import org.btcmap.ui.MapStyleSpec
import org.btcmap.ui.key
import org.btcmap.ui.map.bundledStyleJsonFor
import org.btcmap.util.iconTypeface
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.toUrlOrNull

private const val ARG_KEY = "app_route"
private const val ARG_PLACE_ID = "app_place_id"
private const val ARG_PLACE_NAME = "app_place_name"
private const val ARG_EVENT_ID = "app_event_id"
private const val ARG_EVENT_LAT = "app_event_lat"
private const val ARG_EVENT_LON = "app_event_lon"
private const val ARG_EVENT_NAME = "app_event_name"
private const val ARG_EVENT_WEBSITE = "app_event_website"
private const val ARG_EVENT_STARTS_AT = "app_event_starts_at"
private const val ARG_EVENT_ENDS_AT = "app_event_ends_at"
private const val ARG_LAT = "app_lat"
private const val ARG_LON = "app_lon"
private const val ARG_NAME = "app_name"
private const val ARG_WEBSITE = "app_website"
private const val ARG_STARTS_AT = "app_starts_at"
private const val ARG_ENDS_AT = "app_ends_at"
private const val ARG_REPORT_TYPE = "app_report_type"
private const val ARG_FEED_IDS = "app_feed_ids"
private const val ARG_FEED_NAMES = "app_feed_names"
private const val ARG_FEED_TYPES = "app_feed_types"
private const val ARG_AREA_ID = "app_area_id"

/**
 * Hosts the shared [org.btcmap.ui.AppRoot] for a single migrated screen. The
 * caller picks the start route; the root then owns the navigation among the
 * screens that have moved, and pops this fragment when the user backs out of the
 * start route.
 *
 * This is the bridge between the remaining Android fragments and the Compose
 * root: the map opens one of these instead of the fragment a screen used to be,
 * and the fragment disappears entirely once the whole app is in the root.
 */
class AppRootFragment : Fragment() {

    private var _binding: AppRootFragmentBinding? = null
    private val binding get() = _binding!!

    /** Registered in `onCreate`, before the fragment is STARTED, as the API requires. */
    private lateinit var photoPicker: AndroidPhotoPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoPicker = AndroidPhotoPicker(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AppRootFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val app = context.applicationContext as App
        // A test style pinned by the app is hosted, so it has no bundled JSON and
        // does not carry OpenFreeMap's label font.
        val testStyle = app.mapStyleUriForTesting

        binding.appRoot.apply {
            services = AppServices(
                api = app.api,
                db = app.db,
                settings = prefs,
                syncController = app.syncController,
                imageLoader = SingletonImageLoader.get(context.applicationContext),
                imageHomeDirectory = context.applicationContext.dataDir.path,
                authTokenLabel = authTokenLabel(
                    manufacturer = android.os.Build.MANUFACTURER,
                    model = android.os.Build.MODEL,
                    versionCode = org.btcmap.BuildConfig.VERSION_CODE,
                ),
                offlinePacks = app.offlinePacks,
                offlineStyleUrlFor = { style -> style.offlineStyleUrl(context) },
                offlineStyleMatches = { selected, downloaded ->
                    offlineStyleFamily(downloaded) == offlineStyleFamily(selected)
                },
                usingOpenFreeMap = testStyle == null,
                styleFor = { style ->
                    if (testStyle != null) {
                        MapStyleSpec(testStyle, null)
                    } else {
                        val url = style.uri(context)
                        MapStyleSpec(url, bundledStyleJsonFor(context, url))
                    }
                },
                iconFont = iconTypeface?.let { FontFamily(it) },
            )
            platform = AndroidAppPlatform(
                activity = requireActivity() as org.btcmap.Activity,
                photoPicker = photoPicker::pick,
                openPlaceAction = { placeId ->
                    (requireActivity() as org.btcmap.Activity).openPlace(placeId)
                },
            )
            labels = context.androidAppLabels()
            startRoute = routeFromArgs(arguments)
            onExit = { requireActivity().finish() }
        }

        // System back pops the Compose stack and only closes the app at the map.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!binding.appRoot.back()) {
                requireActivity().finish()
            }
        }
    }

    /** Opens [placeId] on the map, selecting it. */
    fun openPlace(placeId: Long) {
        binding.appRoot.openPlaceId = placeId
    }

    /** Opens [eventId], reading it from the cache or the API. */
    fun openEventById(eventId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val event = withContext(Dispatchers.IO) {
                db().event.selectById(eventId) ?: try {
                    api().getEvent(eventId).toEvent()
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    null
                }
            } ?: return@launch

            binding.appRoot.pendingEvent = event
        }
    }

    /** Whether this host was opened on [route], used to avoid re-opening a screen. */
    fun isShowing(route: AppRoute): Boolean = routeFromArgs(arguments) == route

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Builds a host fragment that opens [route]. */
        fun create(route: AppRoute): AppRootFragment = AppRootFragment().apply {
            arguments = route.toArgs()
        }
    }
}

/** Packs a route and its arguments into a `Bundle` for [AppRootFragment]. */
private fun AppRoute.toArgs(): Bundle = Bundle().apply {
    putString(ARG_KEY, key())
    when (this@toArgs) {
        is AppRoute.Boost -> {
            putLong(ARG_PLACE_ID, placeId)
            putString(ARG_PLACE_NAME, placeName)
        }

        is AppRoute.AddComment -> {
            putLong(ARG_PLACE_ID, placeId)
            putString(ARG_PLACE_NAME, placeName)
        }

        is AppRoute.EventDetails -> {
            putLong(ARG_EVENT_ID, event.id)
            putDouble(ARG_EVENT_LAT, event.lat)
            putDouble(ARG_EVENT_LON, event.lon)
            putString(ARG_EVENT_NAME, event.name)
            putString(ARG_EVENT_WEBSITE, event.website?.toString())
            putString(ARG_EVENT_STARTS_AT, event.startsAt.toString())
            putString(ARG_EVENT_ENDS_AT, event.endsAt?.toString())
        }

        is AppRoute.AddPlace -> {
            putDouble(ARG_LAT, lat)
            putDouble(ARG_LON, lon)
        }

        is AppRoute.AddNote -> {
            putDouble(ARG_LAT, lat)
            putDouble(ARG_LON, lon)
        }

        is AppRoute.AddEvent -> {
            putDouble(ARG_LAT, lat)
            putDouble(ARG_LON, lon)
            putString(ARG_NAME, name)
            putString(ARG_WEBSITE, website)
            putString(ARG_STARTS_AT, startsAt?.toString())
            putString(ARG_ENDS_AT, endsAt?.toString())
        }

        is AppRoute.Report -> {
            putLong(ARG_PLACE_ID, placeId)
            putString(ARG_PLACE_NAME, placeName)
            putString(ARG_REPORT_TYPE, defaultType)
        }

        is AppRoute.Feed -> {
            putStringArrayList(ARG_FEED_IDS, ArrayList(areaIds))
            putStringArrayList(ARG_FEED_NAMES, ArrayList(areaNames))
            putStringArrayList(ARG_FEED_TYPES, ArrayList(areaTypes))
        }

        is AppRoute.Area -> putLong(ARG_AREA_ID, areaId)

        is AppRoute.AreaAdmin -> putLong(ARG_AREA_ID, areaId)

        is AppRoute.Place -> putLong(ARG_PLACE_ID, placeId)

        else -> {}
    }
}

/** Rebuilds the route [AppRootFragment] should open from its arguments. */
private fun routeFromArgs(args: Bundle?): AppRoute = when (args?.getString(ARG_KEY)) {
    "infra-dashboard" -> AppRoute.InfraDashboard
    "event-review" -> AppRoute.EventReview
    "boost" -> AppRoute.Boost(
        placeId = args.getLong(ARG_PLACE_ID),
        placeName = args.getString(ARG_PLACE_NAME).orEmpty(),
    )

    "add-comment" -> AppRoute.AddComment(
        placeId = args.getLong(ARG_PLACE_ID),
        placeName = args.getString(ARG_PLACE_NAME).orEmpty(),
    )

    "event-details" -> AppRoute.EventDetails(
        Event(
            id = args.getLong(ARG_EVENT_ID),
            lat = args.getDouble(ARG_EVENT_LAT),
            lon = args.getDouble(ARG_EVENT_LON),
            name = args.getString(ARG_EVENT_NAME).orEmpty(),
            website = args.getString(ARG_EVENT_WEBSITE)?.toUrlOrNull(),
            startsAt = Instant.parse(requireNotNull(args.getString(ARG_EVENT_STARTS_AT))),
            endsAt = args.getString(ARG_EVENT_ENDS_AT)?.let { Instant.parse(it) },
        ),
    )

    "add-place" -> AppRoute.AddPlace(
        lat = args.getDouble(ARG_LAT),
        lon = args.getDouble(ARG_LON),
    )

    "add-note" -> AppRoute.AddNote(
        lat = args.getDouble(ARG_LAT),
        lon = args.getDouble(ARG_LON),
    )

    "add-event" -> AppRoute.AddEvent(
        lat = args.getDouble(ARG_LAT),
        lon = args.getDouble(ARG_LON),
        name = args.getString(ARG_NAME).orEmpty(),
        website = args.getString(ARG_WEBSITE).orEmpty(),
        startsAt = args.getString(ARG_STARTS_AT)?.let { LocalDateTime.parse(it) },
        endsAt = args.getString(ARG_ENDS_AT)?.let { LocalDateTime.parse(it) },
    )

    "report" -> AppRoute.Report(
        placeId = args.getLong(ARG_PLACE_ID),
        placeName = args.getString(ARG_PLACE_NAME).orEmpty(),
        defaultType = args.getString(ARG_REPORT_TYPE),
    )

    "colors" -> AppRoute.Colors
    "db-stats" -> AppRoute.DbStats
    "image-stats" -> AppRoute.ImageStats

    "feed" -> AppRoute.Feed(
        areaIds = args.getStringArrayList(ARG_FEED_IDS).orEmpty(),
        areaNames = args.getStringArrayList(ARG_FEED_NAMES).orEmpty(),
        areaTypes = args.getStringArrayList(ARG_FEED_TYPES).orEmpty(),
    )

    "area" -> AppRoute.Area(areaId = args.getLong(ARG_AREA_ID))
    "area-admin" -> AppRoute.AreaAdmin(areaId = args.getLong(ARG_AREA_ID))

    "settings" -> AppRoute.Settings
    "manage-areas" -> AppRoute.ManageAreas
    "user-profile" -> AppRoute.UserProfile
    "place" -> AppRoute.Place(placeId = args.getLong(ARG_PLACE_ID))

    else -> AppRoute.Map
}
