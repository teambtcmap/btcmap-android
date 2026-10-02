package org.btcmap.map

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.getEvent
import org.btcmap.api.getPlaceCoordinates
import org.btcmap.api.getPlaceImages
import org.btcmap.api.placeImageUrl
import org.btcmap.area.ARG_AREA_ID
import org.btcmap.area.AreaFragment
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.boost.BoostFragment
import org.btcmap.comment.AddCommentFragment
import org.btcmap.comment.CommentsFragment
import org.btcmap.db
import org.btcmap.db.table.place.Place
import org.btcmap.databinding.MapFragmentBinding
import org.btcmap.event.EventFragment
import org.btcmap.event.toBundle
import org.btcmap.feed.ActivityFeedFragment
import org.btcmap.i18n.getLocalizedName
import org.btcmap.place.AddPlaceFragment
import org.btcmap.place.ReportPlaceFragment
import org.btcmap.place.btcmapUrl
import org.btcmap.place.osmEditUrl
import org.btcmap.place.osmMapUrl
import org.btcmap.place.PlacePhotoUploader
import org.btcmap.place.osmUrl
import org.btcmap.saved.isPlaceSaved
import org.btcmap.saved.toggleSavedPlace
import org.btcmap.settings.SettingsFragment
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.boostedMarkerIconColor
import org.btcmap.settings.buttonBackgroundColor
import org.btcmap.settings.buttonBorderColor
import org.btcmap.settings.buttonIconColor
import org.btcmap.settings.mapCenterLat
import org.btcmap.settings.mapCenterLon
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.mapStyle
import org.btcmap.settings.mapZoom
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.markerIconColor
import org.btcmap.settings.prefs
import org.btcmap.settings.showAttribution
import org.btcmap.settings.uri
import org.btcmap.settings.verifiedFilterMinVerifiedAt
import org.btcmap.sync.SyncState
import org.btcmap.syncController
import org.btcmap.ui.PlaceAction
import org.btcmap.ui.PlaceSheetStrings
import org.btcmap.ui.map.SearchActions
import org.btcmap.ui.map.bundledStyleJsonFor
import org.btcmap.util.DeepLink
import org.btcmap.util.iconTypeface
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation
import java.text.NumberFormat

class MapFragment : Fragment() {
    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private var updateNotificationController: UpdateNotificationController? = null

    private val photoUploader = PlacePhotoUploader(
        fragment = this,
        onAuthRequired = { placeId, placeName ->
            showAuthDialog(
                Bundle().apply {
                    putString(EXTRA_AUTH_ACTION, AUTH_ACTION_ADD_PHOTO)
                    putLong(EXTRA_PLACE_ID, placeId)
                    putString(EXTRA_PLACE_NAME, placeName)
                },
            )
        },
        onUploaded = { placeId ->
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) { db().place.selectById(placeId) }
                    ?.let { refreshSheet(it) }
            }
        },
        onError = { showError(it) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = MapFragmentBinding.inflate(inflater, container, false)
        initInsets(binding)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The place sheet's actions can need a signed-in user; the map asks for
        // one here and finishes the action when the form comes back.
        registerAuthResultListener { extras -> onAuthResult(extras) }

        updateNotificationController = UpdateNotificationController(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            icon = binding.update,
        )

        setUpMap()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // The sync is app-scoped: starting it here only kicks it off, and
                // it keeps running after this screen is closed. Each resume starts
                // a fresh one unless the previous run is still going.
                syncController().start()

                launch {
                    syncController().state.collect { state ->
                        binding.sync.setVisibleAnimated(state != SyncState.Idle)
                    }
                }

                syncController().events.collect {
                    // The shared map reads its features when the camera settles,
                    // so a finished sync asks it to query them again instead of
                    // leaving the map stale until the user moves it.
                    binding.map.reloadKey++
                }
            }
        }

        val deepLink = (activity as? Activity)?.consumeDeepLink()
        when (deepLink) {
            is DeepLink.Place -> openPlaceById(deepLink.id)
            is DeepLink.Event -> openEventById(deepLink.id)
            null -> {}
        }

        // After the deep link, so a fresh link still wins: the restore only runs
        // on a recreation, where the Activity does not re-deliver the link.
        restoreSelectedPlace(savedInstanceState)
    }

    /**
     * Hands the shared map everything it needs from this host: the database, the
     * style, the stored camera and the app's colours, plus the callbacks for
     * what stays Android-side.
     */
    private fun setUpMap() {
        val app = requireContext().applicationContext as App
        val styleUrl = app.mapStyleUriForTesting ?: prefs.mapStyle.uri(requireContext())
        // Read before apply(): inside it, the name would resolve to the view's
        // own property instead of the icon font lazily built from the assets.
        val typeface = iconTypeface

        binding.map.apply {
            database = db()
            this.styleUrl = styleUrl
            styleJson = if (app.mapStyleUriForTesting == null) {
                bundledStyleJsonFor(requireContext(), styleUrl)
            } else {
                null
            }
            initialLat = prefs.mapCenterLat
            initialLon = prefs.mapCenterLon
            initialZoom = prefs.mapZoom
            minVerifiedAt = prefs.verifiedFilterMinVerifiedAt()
            apiUrl = prefs.apiUrl.toString()
            // Every selectable style draws from OpenFreeMap, so it carries the
            // Noto Sans Bold font the cluster counts need. A test style pinned
            // via mapStyleUriForTesting does not.
            usingOpenFreeMap = app.mapStyleUriForTesting == null
            mapRotationEnabled = prefs.mapRotationEnabled
            iconTypeface = typeface
            markerBackgroundColor = Color(prefs.markerBackgroundColor(requireContext()))
            markerIconColor = Color(prefs.markerIconColor(requireContext()))
            boostedMarkerBackgroundColor = Color(prefs.boostedMarkerBackgroundColor())
            boostedMarkerIconColor = Color(prefs.boostedMarkerIconColor())
            markerBadgeBackgroundColor = Color(prefs.badgeBackgroundColor(requireContext()))
            markerBadgeTextColor = Color(prefs.badgeTextColor(requireContext()))
            areaChipButtonColor = Color(prefs.buttonBackgroundColor(requireContext()))
            areaChipIconColor = Color(prefs.buttonIconColor(requireContext()))
            areaChipBorderColor = Color(prefs.buttonBorderColor(requireContext()))
            placeSheetStrings = PlaceSheetStrings(
                directions = getString(R.string.directions),
                share = getString(R.string.share),
                viewOnBtcmap = getString(R.string.view_on_btcmap),
                viewOnOsm = getString(R.string.view_on_osm),
                editOnOsm = getString(R.string.edit_on_osm),
                notVerified = getString(R.string.not_verified),
                verificationWarningTitle = getString(R.string.verification_warning_title),
                verificationWarningOutdated = getString(R.string.verification_warning_outdated),
                verificationWarningNotVerified = getString(R.string.verification_warning_not_verified),
                ok = getString(android.R.string.ok),
                companionWarning = { getString(R.string.companion_warning, it) },
                verify = getString(R.string.btn_verify),
                report = getString(R.string.btn_report),
                boost = getString(R.string.boost),
                comments = { count ->
                    if (count == 0L) {
                        getString(R.string.comments)
                    } else {
                        getString(R.string.comments_d, count.toInt())
                    }
                },
                commentsTitle = { count -> getString(R.string.comments_d, count.toInt()) },
                addComment = getString(R.string.add_comment),
                save = getString(R.string.save),
                addPhoto = getString(R.string.add_photo),
            )
            showAttribution = prefs.showAttribution
            attributionText = getString(R.string.osm_attribution)
            attributionTextColor = Color(
                ContextCompat.getColor(requireContext(), R.color.osm_attribution_text),
            )
            onPlaceSelected = ::onPlaceSelected
            onPlaceDismissed = { selectedPlaceId = null }
            onPlaceAction = ::onPlaceAction
            onEventSelected = { openEvent(it.toBundle()) }
            onAreaSelected = ::openArea
            onOpenFeed = ::openFeed
            onAddPlace = { lat, lon ->
                pendingAddPlace = lat to lon
                if (prefs.authorized) navigateToAddPlace(lat, lon) else showAuthDialog()
            }
            searchActions = SearchActions(onSettings = { navigateToSettings() })
            formatDistance = ::formatDistance
            onFeaturesDrawn = { (activity as? Activity)?.reportFullyDrawn() }
            onCameraIdle = { lat, lon, zoom ->
                // Recorded so the next launch reopens what the user was looking
                // at. The first idle is the stored camera itself, so this is a
                // no-op until the map is actually moved.
                prefs.mapCenterLat = lat
                prefs.mapCenterLon = lon
                prefs.mapZoom = zoom
            }
        }
    }

    /**
     * Re-opens the sheet on the place it was showing before the view was
     * recreated, so a rotation does not dismiss it.
     */
    private fun restoreSelectedPlace(savedInstanceState: Bundle?) {
        val placeId = savedInstanceState?.getLong(STATE_PLACE_ID, 0L) ?: selectedPlaceId ?: 0L
        if (placeId <= 0L) return

        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) { db().place.selectById(placeId) }
                ?: return@launch
            binding.map.openPlaceId = place.id
        }
    }

    private fun onPlaceSelected(place: Place) {
        selectedPlaceId = place.id
        refreshSheet(place)
    }

    /** Fills in what only the app can supply the sheet: its photos and its bookmark. */
    private fun refreshSheet(place: Place) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.map.bookmarked = isPlaceSaved(place.id)
            val images = try {
                api().getPlaceImages(place.id)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                emptyList()
            }
            binding.map.photos = images.map {
                api().placeImageUrl(place.id, it.id, width = 144, height = 144)
            }
        }
    }

    private fun onPlaceAction(place: Place, action: PlaceAction) {
        when (action) {
            PlaceAction.Directions -> startActivity(
                Intent.createChooser(
                    Intent(
                        Intent.ACTION_VIEW,
                        "geo:${place.lat},${place.lon}?q=${place.getLocalizedName()}".toUri(),
                    ),
                    null,
                ),
            )

            PlaceAction.Share -> startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, place.btcmapUrl())
                        type = "text/plain"
                    },
                    null,
                ),
            )

            PlaceAction.ViewOnBtcmap -> openOnBtcmap(place.btcmapUrl())

            PlaceAction.ViewOnOsm -> place.osmUrl()?.let { openInBrowser(it.toUri()) }

            PlaceAction.EditOnOsm -> place.osmEditUrl()?.let { openInBrowser(it.toUri()) }

            PlaceAction.ToggleBookmark -> {
                if (prefs.authorized) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            toggleSavedPlace(place.id, place.name.orEmpty())
                            binding.map.bookmarked = isPlaceSaved(place.id)
                        } catch (t: Throwable) {
                            t.rethrowIfCancellation()
                            showError(t)
                        }
                    }
                } else {
                    showAuthDialog(
                        Bundle().apply {
                            putString(EXTRA_AUTH_ACTION, AUTH_ACTION_TOGGLE_SAVED)
                            putLong(EXTRA_PLACE_ID, place.id)
                            putString(EXTRA_PLACE_NAME, place.name.orEmpty())
                        },
                    )
                }
            }

            PlaceAction.Verify -> openReport(place, defaultType = "verified")

            PlaceAction.Report -> openReport(place, defaultType = null)

            PlaceAction.Boost -> navigate(
                BoostFragment(),
                Bundle().apply {
                    putLong("place_id", place.id)
                    putString("place_name", place.name.orEmpty())
                },
            )

            PlaceAction.Comments -> viewLifecycleOwner.lifecycleScope.launch {
                val hasComments = withContext(Dispatchers.IO) {
                    db().comment.selectCountByPlaceId(place.id) > 0
                }
                if (hasComments) openComments(place) else openAddComment(place)
            }

            PlaceAction.AddComment -> openAddComment(place)

            PlaceAction.AddPhoto -> requestAddPhoto(place)
        }
    }

    private fun onAuthResult(extras: Bundle) {
        when (extras.getString(EXTRA_AUTH_ACTION)) {
            AUTH_ACTION_TOGGLE_SAVED -> {
                val placeId = extras.getLong(EXTRA_PLACE_ID, 0L)
                val placeName = extras.getString(EXTRA_PLACE_NAME) ?: return
                if (placeId <= 0L) return
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        toggleSavedPlace(placeId, placeName)
                        if (selectedPlaceId == placeId) binding.map.bookmarked = isPlaceSaved(placeId)
                    } catch (t: Throwable) {
                        t.rethrowIfCancellation()
                        showError(t)
                    }
                }
            }

            AUTH_ACTION_ADD_PHOTO -> {
                val placeId = extras.getLong(EXTRA_PLACE_ID, 0L)
                if (placeId <= 0L) return
                photoUploader.resumeAfterAuth(placeId)
            }

            AUTH_ACTION_OPEN_REPORT -> viewLifecycleOwner.lifecycleScope.launch {
                val placeId = extras.getLong(EXTRA_PLACE_ID, 0L)
                if (placeId <= 0L) return@launch
                val place = withContext(Dispatchers.IO) { db().place.selectById(placeId) }
                    ?: return@launch
                navigateToReport(place, extras.getString(EXTRA_REPORT_TYPE))
            }

            // No action means the map only asked the user to sign in: the action
            // that needed it was adding a place.
            else -> addPlaceAfterAuth()
        }
    }

    /**
     * Uploads require a signed-in user, so prompt for auth first if needed. The
     * picker, camera and upload are the shared [PlacePhotoUploader].
     */
    private fun requestAddPhoto(place: Place) {
        photoUploader.request(place.id, place.name.orEmpty())
    }

    private fun openReport(place: Place, defaultType: String?) {
        if (prefs.authorized) {
            navigateToReport(place, defaultType)
        } else {
            showAuthDialog(
                Bundle().apply {
                    putString(EXTRA_AUTH_ACTION, AUTH_ACTION_OPEN_REPORT)
                    putLong(EXTRA_PLACE_ID, place.id)
                    putString(EXTRA_PLACE_NAME, place.name.orEmpty())
                    if (defaultType != null) putString(EXTRA_REPORT_TYPE, defaultType)
                },
            )
        }
    }

    private fun navigateToReport(place: Place, defaultType: String?) {
        navigate(
            ReportPlaceFragment(),
            Bundle().apply {
                putLong("place_id", place.id)
                putString("place_name", place.name.orEmpty())
                if (defaultType != null) putString("default_type", defaultType)
            },
        )
    }

    private fun openComments(place: Place) {
        navigate(CommentsFragment(), placeArgs(place))
    }

    private fun openAddComment(place: Place) {
        navigate(AddCommentFragment(), placeArgs(place))
    }

    private fun placeArgs(place: Place): Bundle = Bundle().apply {
        putLong("place_id", place.id)
        putString("place_name", place.name.orEmpty())
    }

    /**
     * Opens the place on btcmap.org in a Custom Tab.
     *
     * The browser is targeted explicitly: a plain VIEW intent would be captured
     * by this app's own verified App Link for btcmap.org/merchant and reopen the
     * place instead of the site. If no Custom Tabs browser is available, fall
     * back to a chooser that excludes this app, for the same reason.
     */
    private fun openOnBtcmap(url: String) {
        val uri = url.toUri()

        CustomTabsClient.getPackageName(requireContext(), null)?.let { browser ->
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(browser)
            try {
                customTabs.launchUrl(requireContext(), uri)
                return
            } catch (_: ActivityNotFoundException) {
                // Fall through to the chooser below.
            }
        }

        val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), null)
        chooser.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(ComponentName(requireContext(), Activity::class.java)),
        )
        try {
            startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            showError(e)
        }
    }

    private fun navigate(fragment: Fragment, args: Bundle?) {
        fragment.arguments = args
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainerView, fragment)
            addToBackStack(null)
        }
    }

    private fun showError(t: Throwable) {
        Toast.makeText(requireContext(), t.message ?: t.toString(), Toast.LENGTH_LONG).show()
    }

    private fun openEvent(bundle: Bundle) {
        parentFragmentManager.executePendingTransactions()

        val current = parentFragmentManager.findFragmentById(R.id.fragmentContainerView)
        if (current is EventFragment && current.eventId == bundle.getLong("id")) return

        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<EventFragment>(R.id.fragmentContainerView, null, bundle)
            addToBackStack(null)
        }
    }

    fun openEventById(eventId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bundle = withContext(Dispatchers.IO) {
                    val local = db().event.selectById(eventId)?.toBundle()
                    if (local != null) {
                        local
                    } else {
                        try {
                            api().getEvent(eventId).toBundle()
                        } catch (t: Throwable) {
                            t.rethrowIfCancellation()
                            null
                        }
                    }
                } ?: return@launch

                openEvent(bundle)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
            }
        }
    }

    fun openPlaceById(placeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) { db().place.selectById(placeId) }

            if (place != null) {
                // The shared map selects it, opens the sheet and moves to it.
                binding.map.openPlaceId = place.id
                return@launch
            }

            val coordinates = try {
                withContext(Dispatchers.IO) { api().getPlaceCoordinates(placeId) }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                null
            } ?: return@launch

            // Not cached yet: there is no place to select, so the map only moves.
            binding.map.openTarget = coordinates.lat to coordinates.lon
        }
    }

    private fun openArea(areaId: Long) {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<AreaFragment>(
                R.id.fragmentContainerView, null,
                Bundle().apply { putLong(ARG_AREA_ID, areaId) },
            )
            addToBackStack(null)
        }
    }

    private fun openFeed(areas: List<org.btcmap.map.MapArea>) {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<ActivityFeedFragment>(
                R.id.fragmentContainerView, null, Bundle().apply {
                    putStringArrayList("area_ids", ArrayList(areas.map { it.urlAlias }))
                    putStringArrayList("area_names", ArrayList(areas.map { it.name }))
                    putStringArrayList("area_types", ArrayList(areas.map { it.type }))
                }
            )
            addToBackStack(null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedPlaceId?.let { outState.putLong(STATE_PLACE_ID, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateNotificationController = null
        _binding = null
    }

    /**
     * The place the sheet is showing, or null when nothing is selected. Kept so a
     * rotation (through the saved state) or a back-stack return (through the
     * field) reopens the same place.
     */
    private var selectedPlaceId: Long? = null

    /**
     * The map centre an add-place that still has to authenticate should use once
     * the form comes back signed in.
     */
    private var pendingAddPlace: Pair<Double, Double>? = null

    private fun addPlaceAfterAuth() {
        val pending = pendingAddPlace ?: return
        navigateToAddPlace(pending.first, pending.second)
    }

    /** Mirrors the distance labels the Views search showed. */
    private fun formatDistance(meters: Double): String {
        val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
        return if (meters < 1_000) {
            getString(R.string.s_m, format.format(meters))
        } else {
            getString(R.string.s_km, format.format(meters / 1_000))
        }
    }

    private fun initInsets(binding: MapFragmentBinding) {
        // The search field, the chips, the marker filter and the location button
        // are the shared map's own, so only the sync and update indicators are
        // inset here. They belong above the filter column, as they did in the
        // Views button group, so the bottom margin clears the column rather than
        // sitting the group in the bottom corner under it.
        ViewCompat.setOnApplyWindowInsetsListener(binding.buttonGroup) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                rightMargin = insets.right
                bottomMargin = dpToPx(FILTER_COLUMN_CLEARANCE_DP)
                leftMargin = insets.left + dpToPx(24)
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun navigateToSettings() {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<SettingsFragment>(R.id.fragmentContainerView)
            addToBackStack(null)
        }
    }

    private fun navigateToAddPlace(lat: Double, lon: Double) {
        pendingAddPlace = null
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<AddPlaceFragment>(
                R.id.fragmentContainerView, null, Bundle().apply {
                    putDouble("lat", lat)
                    putDouble("lon", lon)
                }
            )
            addToBackStack(null)
        }
    }

    private companion object {
        private const val STATE_PLACE_ID = "map_selected_place_id"

        // The shared map's marker filter column reserves this much from the map's
        // bottom edge: MapScreen's MAP_CONTROLS_BOTTOM (52dp, just above the
        // attribution), the three 48dp buttons 8dp apart (MarkerFilterButtons),
        // and the 8dp gap the sync group left it.
        private const val FILTER_COLUMN_CLEARANCE_DP = 52 + 3 * 48 + 2 * 8 + 8

        // The auth hand-off protocol, the same shape the place screen uses: what
        // the sheet asked for, and what the form has to hand back to finish it.
        private const val EXTRA_AUTH_ACTION = "auth-action"
        private const val EXTRA_PLACE_ID = "auth-place-id"
        private const val EXTRA_PLACE_NAME = "auth-place-name"
        private const val EXTRA_REPORT_TYPE = "auth-report-type"

        private const val AUTH_ACTION_TOGGLE_SAVED = "toggle-saved"
        private const val AUTH_ACTION_OPEN_REPORT = "open-report"
        private const val AUTH_ACTION_ADD_PHOTO = "add-photo"
    }
}
