package org.btcmap.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.getEvent
import org.btcmap.api.getPlaceCoordinates
import org.btcmap.area.ARG_AREA_ID
import org.btcmap.area.AreaFragment
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
import org.btcmap.databinding.MapFragmentBinding
import org.btcmap.event.EventFragment
import org.btcmap.event.toBundle
import org.btcmap.feed.ActivityFeedFragment
import org.btcmap.place.AddPlaceFragment
import org.btcmap.db.table.place.Place
import org.btcmap.place.PlaceFragment
import org.btcmap.settings.SettingsFragment
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.boostedMarkerIconColor
import org.btcmap.settings.buttonBackgroundColor
import org.btcmap.settings.buttonIconColor
import org.btcmap.settings.mapCenterLat
import org.btcmap.settings.mapCenterLon
import org.btcmap.settings.mapStyle
import org.btcmap.settings.mapZoom
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.markerIconColor
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.btcmap.settings.verifiedFilterMinVerifiedAt
import org.btcmap.sync.SyncState
import org.btcmap.syncController
import org.btcmap.ui.map.SearchActions
import org.btcmap.ui.map.bundledStyleJsonFor
import org.btcmap.util.DeepLink
import org.btcmap.util.iconTypeface
import org.btcmap.util.rethrowIfCancellation
import java.text.NumberFormat

class MapFragment : Fragment() {
    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private var statusBarController: MapStatusBarController? = null
    private var bottomSheetController: BottomSheetController? = null
    private var updateNotificationController: UpdateNotificationController? = null

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

        // Registered here, not when the dialog is shown, so a form that was open
        // when the device rotated still reaches this (recreated) fragment.
        registerAuthResultListener { addPlaceAfterAuth() }

        val bottomSheet = BottomSheetController(
            view = binding.placeBottomSheet,
            viewLifecycleOwner = viewLifecycleOwner,
            placeFragment = childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment,
        )
        bottomSheetController = bottomSheet

        statusBarController = MapStatusBarController(
            conf = resources.configuration,
            insetsController = WindowCompat.getInsetsController(
                requireActivity().window,
                requireActivity().window.decorView,
            ),
            bottomSheetBehavior = bottomSheet.bottomSheetBehavior,
        )
        statusBarController?.onViewCreated()

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
        restoreBottomSheet(savedInstanceState)
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
            iconTypeface = typeface
            markerBackgroundColor = Color(prefs.markerBackgroundColor(requireContext()))
            markerIconColor = Color(prefs.markerIconColor(requireContext()))
            boostedMarkerBackgroundColor = Color(prefs.boostedMarkerBackgroundColor())
            boostedMarkerIconColor = Color(prefs.boostedMarkerIconColor())
            markerBadgeBackgroundColor = Color(prefs.badgeBackgroundColor(requireContext()))
            markerBadgeTextColor = Color(prefs.badgeTextColor(requireContext()))
            areaChipButtonColor = Color(prefs.buttonBackgroundColor(requireContext()))
            areaChipIconColor = Color(prefs.buttonIconColor(requireContext()))
            // The place screen is still a Views screen: verify, report, boost
            // and the photo flows live there, so this host keeps its own sheet
            // and borrows only the map.
            placeSheet = false
            onPlaceSelected = ::selectPlace
            onEventSelected = { openEvent(it.toBundle()) }
            onAreaSelected = ::openArea
            onOpenFeed = ::openFeed
            onAddPlace = { lat, lon ->
                pendingAddPlace = lat to lon
                if (prefs.authorized) navigateToAddPlace(lat, lon) else showAuthDialog()
            }
            searchActions = SearchActions(onSettings = { navigateToSettings() })
            formatDistance = ::formatDistance
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
     * Re-selects the place the sheet was showing before the view was recreated,
     * and restores its position, so a rotation or a return from another screen
     * does not dismiss it. A hidden sheet is left closed: the user dismissed it,
     * or nothing was selected.
     */
    private fun restoreBottomSheet(savedInstanceState: Bundle?) {
        // A rotation delivers the values through the saved state. A back-stack
        // return has none (the fragment instance survives, only its view is
        // recreated), so they come from the retained fields instead.
        val placeId = savedInstanceState?.getLong(STATE_PLACE_ID, 0L) ?: selectedPlaceId ?: 0L
        val sheetState =
            savedInstanceState?.getInt(STATE_SHEET_STATE, BottomSheetBehavior.STATE_HIDDEN)
                ?: lastSheetState
        if (placeId <= 0L || sheetState == BottomSheetBehavior.STATE_HIDDEN) return

        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) { db().place.selectById(placeId) }
                ?: return@launch

            selectPlace(place)
            if (sheetState == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetController?.bottomSheetBehavior?.state =
                    BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun selectPlace(place: Place) {
        selectedPlaceId = place.id
        val placeFragment =
            childFragmentManager.findFragmentById(R.id.placeFragment) as PlaceFragment
        placeFragment.setPlace(place)
        bottomSheetController?.halfExpand()
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

    private fun openFeed(areas: List<MapArea>) {
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
        outState.putInt(
            STATE_SHEET_STATE,
            bottomSheetController?.bottomSheetBehavior?.state
                ?: BottomSheetBehavior.STATE_HIDDEN,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remembered so a back-stack return can restore the sheet, which has no
        // saved instance state to read from.
        lastSheetState = bottomSheetController?.bottomSheetBehavior?.state
            ?: BottomSheetBehavior.STATE_HIDDEN
        bottomSheetController = null
        statusBarController?.onDestroyView()
        statusBarController = null
        updateNotificationController = null
        _binding = null
    }

    /**
     * The place the bottom sheet is showing, or null when nothing is selected.
     * Saved with the view so a rotation keeps the sheet's place and position
     * instead of dismissing it.
     */
    private var selectedPlaceId: Long? = null

    /**
     * The sheet's state when the view was last destroyed. A back-stack return
     * keeps this fragment instance but has no saved instance state, so this is
     * what tells [restoreBottomSheet] whether the sheet was visible (and how).
     * A rotation reads the state from the saved bundle instead.
     */
    private var lastSheetState = BottomSheetBehavior.STATE_HIDDEN

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
        // The search field, the chips and the location button are the shared
        // map's own, so only the sync and update buttons are inset here.
        ViewCompat.setOnApplyWindowInsetsListener(binding.buttonGroup) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                rightMargin = insets.right
                bottomMargin = insets.bottom + dpToPx(20)
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

        private const val STATE_SHEET_STATE = "map_sheet_state"
    }
}
