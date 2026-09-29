package org.btcmap.place

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.Activity
import org.btcmap.api
import org.btcmap.api.getPlaceCoordinates
import org.btcmap.app
import org.btcmap.boost.BoostFragment
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.place.toMarker
import org.btcmap.map.markerImageName
import org.btcmap.map.merchantMarkerBitmap
import org.btcmap.settings.prefs
import org.btcmap.comment.AddCommentFragment
import org.btcmap.comment.CommentsAdapter
import org.btcmap.comment.commentDateFormatter
import org.btcmap.comment.toAdapterItem
import org.btcmap.comment.CommentsFragment
import org.btcmap.util.iconTypeface
import org.btcmap.util.showError
import org.btcmap.map.getErrorColor
import org.btcmap.map.getOnSurfaceColor
import org.btcmap.openinghours.OpeningHours
import org.btcmap.openinghours.toOpeningHours
import org.btcmap.R
import org.btcmap.SyncEvent
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
import org.btcmap.databinding.PlaceFragmentBinding
import org.btcmap.i18n.getLocalizedName
import org.btcmap.saved.isPlaceSaved
import org.btcmap.saved.toggleSavedPlace
import org.btcmap.settings.authorized
import org.btcmap.settings.badgeBackgroundColor
import org.btcmap.settings.badgeTextColor
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.mapStyle
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.uri
import org.btcmap.syncController
import org.btcmap.util.rethrowIfCancellation
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.abs

class PlaceFragment : Fragment() {

    companion object {
        /**
         * Marks a standalone place screen: one opened outside the map's bottom
         * sheet. It carries the place to show and turns on the preview map.
         */
        const val ARG_PLACE_ID = "place_id"

        private const val EXTRA_AUTH_ACTION = "auth-action"
        private const val EXTRA_PLACE_ID = "auth-place-id"
        private const val EXTRA_PLACE_NAME = "auth-place-name"
        private const val EXTRA_REPORT_TYPE = "auth-report-type"

        private const val AUTH_ACTION_TOGGLE_SAVED = "toggle-saved"
        private const val AUTH_ACTION_OPEN_REPORT = "open-report"

        /**
         * How many comments the place screen previews inline. A place's full
         * list is unbounded, so the preview query is capped here to keep the
         * sheet's RecyclerView from inflating with every comment; the comments
         * button shows the total and opens them all.
         */
        private const val COMMENTS_PREVIEW_LIMIT = 3L

        private const val SMALL_MAP_ZOOM = 16.0
        private const val SMALL_MAP_DEFAULT_MARKER_IMAGE = "place-preview-marker"
        private const val SMALL_MAP_SOURCE_ID = "place_preview_source"
        private const val SMALL_MAP_LAYER_ID = "place_preview_marker"

        /** A place screen with its own preview map, not hosted by the map. */
        fun create(placeId: Long): PlaceFragment {
            return PlaceFragment().apply {
                arguments = Bundle().apply { putLong(ARG_PLACE_ID, placeId) }
            }
        }
    }

    private var placeId = 0L

    private var placeName = ""

    private lateinit var commentsAdapter: CommentsAdapter

    private var commentsJob: Job? = null

    private var smallMap: MapLibreMap? = null
    private var smallMapCreated = false
    private var previewPlace: Place? = null
    private var previewLatLng: LatLng? = null
    private var previewTouchDownX = 0f
    private var previewTouchDownY = 0f

    private val isStandalone: Boolean
        get() = arguments?.containsKey(ARG_PLACE_ID) == true

    private val requestedPlaceId: Long
        get() = arguments?.getLong(ARG_PLACE_ID, 0L) ?: 0L

    private var _binding: PlaceFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PlaceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Registered here, not when the dialog is shown, so a form that was open
        // when the device rotated still reaches this (recreated) fragment.
        registerAuthResultListener { extras ->
            when (extras.getString(EXTRA_AUTH_ACTION)) {
                AUTH_ACTION_TOGGLE_SAVED -> toggleSavedPlaceNow(
                    placeId = extras.getLong(EXTRA_PLACE_ID, placeId),
                    placeName = extras.getString(EXTRA_PLACE_NAME) ?: placeName,
                )

                AUTH_ACTION_OPEN_REPORT -> navigateToReport(
                    placeId = extras.getLong(EXTRA_PLACE_ID, placeId),
                    defaultType = extras.getString(EXTRA_REPORT_TYPE),
                )
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.directions -> {
                    // Read the coordinates off the main thread: the shared
                    // connection's lock can stall a read behind a background
                    // sync write, and this handler runs on the main thread.
                    viewLifecycleOwner.lifecycleScope.launch {
                        val place = withContext(Dispatchers.IO) { db().place.selectById(placeId) }
                            ?: return@launch
                        val uri = "geo:${place.lat},${place.lon}?q=${place.getLocalizedName()}".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        requireContext().startActivity(Intent.createChooser(intent, null))
                    }
                }

                R.id.share -> {
                    val uri = "https://btcmap.org/merchant/$placeId".toUri()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, uri.toString())
                        type = "text/plain"
                    }
                    requireContext().startActivity(Intent.createChooser(intent, null))
                }

                R.id.view_on_btcmap -> openOnBtcmap()

                R.id.save -> onSaveClicked()
            }

            true
        }

        binding.outdated.typeface = iconTypeface
        binding.outdated.setTextColor(requireContext().getErrorColor())

        binding.companionWarning.setTextColor(requireContext().getErrorColor())
        TextViewCompat.setCompoundDrawableTintList(
            binding.companionWarning,
            ColorStateList.valueOf(requireContext().getErrorColor()),
        )

        commentsAdapter = CommentsAdapter()
        binding.commentsList.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsList.adapter = commentsAdapter

        // The place is read from the local cache, so a deep link opened while
        // the row is still the pre-sync copy (or any background sync that
        // rewrites it) makes this screen stale. Re-render when the sync reports
        // the place table changed instead of leaving the outdated copy up until
        // the screen is reopened.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncController().events.collect { event ->
                    if (event == SyncEvent.PlacesChanged) reloadPlace()
                }
            }
        }

        if (isStandalone) {
            setUpStandalone(savedInstanceState)
        }
    }

    /**
     * A standalone place screen is not hosted by the map, so it owns a small
     * preview map of its own and needs its own back affordance: in the map's
     * bottom sheet the sheet handles back.
     *
     * The preview map is a MapView, which consumes touch events itself, so it
     * needs an explicit touch listener rather than a click listener.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setUpStandalone(savedInstanceState: Bundle?) {
        // The map's bottom sheet positions the toolbar itself, but a standalone
        // screen owns the status bar inset or the toolbar slides under it.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val top = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val vertical = resources.getDimensionPixelSize(R.dimen.place_toolbar_vertical_padding)
            binding.toolbar.updatePadding(top = top + vertical, bottom = vertical)
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.navigationIcon = homeAsUpIndicator()

        binding.map.visibility = View.VISIBLE
        binding.map.setOnTouchListener(::onPreviewMapTouch)
        binding.map.onCreate(savedInstanceState)
        smallMapCreated = true
        initSmallMap()
        loadStandalonePlace()
    }

    private fun homeAsUpIndicator(): Drawable? {
        val value = TypedValue()
        val resolved = requireContext().theme.resolveAttribute(
            androidx.appcompat.R.attr.homeAsUpIndicator,
            value,
            true,
        )
        return if (resolved) {
            AppCompatResources.getDrawable(requireContext(), value.resourceId)
        } else {
            null
        }
    }

    private fun initSmallMap() {
        binding.map.getMapAsync { map ->
            if (_binding == null) return@getMapAsync
            smallMap = map
            map.setStyle(Style.Builder().fromUri(styleUri()))
            map.uiSettings.apply {
                setAllGesturesEnabled(false)
                isLogoEnabled = false
                isAttributionEnabled = false
                isCompassEnabled = false
            }
            // The place may have loaded before the map was ready, so render now
            // as well; renderSmallMap is idempotent.
            renderSmallMap()
        }
    }

    private fun styleUri(): String {
        return app().mapStyleUriForTesting ?: prefs.mapStyle.uri(requireContext())
    }

    /**
     * Adds the place's composited marker to the preview style and returns the
     * image name to draw. Falls back to a plain pin when the place has not
     * synced yet and only its coordinates are known.
     */
    private fun previewMarkerImage(style: Style): String {
        val place = previewPlace ?: return defaultPreviewMarkerImage(style)

        val marker = place.toMarker()
        val name = marker.markerImageName()
        if (style.getImage(name) == null) {
            style.addImage(
                name,
                merchantMarkerBitmap(
                    context = requireContext(),
                    marker = marker,
                    markerBackgroundColor = prefs.markerBackgroundColor(requireContext()),
                    boostedMarkerBackgroundColor = prefs.boostedMarkerBackgroundColor(),
                    markerBadgeBackgroundColor = prefs.badgeBackgroundColor(requireContext()),
                    markerBadgeTextColor = prefs.badgeTextColor(requireContext()),
                ),
            )
        }
        return name
    }

    private fun defaultPreviewMarkerImage(style: Style): String {
        if (style.getImage(SMALL_MAP_DEFAULT_MARKER_IMAGE) == null) {
            val drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.map_marker)!!
                .mutate()
            DrawableCompat.setTint(drawable, prefs.markerBackgroundColor(requireContext()))
            style.addImage(SMALL_MAP_DEFAULT_MARKER_IMAGE, drawable)
        }
        return SMALL_MAP_DEFAULT_MARKER_IMAGE
    }

    /** Centres the preview map on the place and draws its marker. */
    private fun renderSmallMap() {
        val target = previewLatLng ?: return
        val map = smallMap ?: return

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, SMALL_MAP_ZOOM))
        map.getStyle { style ->
            if (_binding == null) return@getStyle
            renderSmallMapMarker(style, target, previewMarkerImage(style))
        }
    }

    private fun renderSmallMapMarker(style: Style, target: LatLng, imageName: String) {
        val point = Point.fromLngLat(target.longitude, target.latitude)

        val source = style.getSource(SMALL_MAP_SOURCE_ID) as? GeoJsonSource
        if (source == null) {
            style.addSource(GeoJsonSource(SMALL_MAP_SOURCE_ID, point))
        } else {
            source.setGeoJson(point)
        }

        if (style.getLayer(SMALL_MAP_LAYER_ID) == null) {
            style.addLayer(
                SymbolLayer(SMALL_MAP_LAYER_ID, SMALL_MAP_SOURCE_ID).withProperties(
                    PropertyFactory.iconImage(imageName),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                )
            )
        }
    }

    /**
     * The preview map is non-interactive, so a tap on it asks for the place to
     * be opened on the real map. A tap is told apart from a scroll by touch
     * slop, and the listener returns false so the parent can still scroll.
     */
    private fun onPreviewMapTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previewTouchDownX = event.x
                previewTouchDownY = event.y
            }

            MotionEvent.ACTION_UP -> {
                val slop = ViewConfiguration.get(view.context).scaledTouchSlop
                val placeId = requestedPlaceId
                if (placeId > 0L &&
                    abs(event.x - previewTouchDownX) <= slop &&
                    abs(event.y - previewTouchDownY) <= slop
                ) {
                    (activity as? Activity)?.openPlace(placeId)
                }
            }
        }

        return false
    }

    private fun loadStandalonePlace() {
        val requestedId = requestedPlaceId
        if (requestedId <= 0L) return

        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) { db().place.selectById(requestedId) }
            if (place != null) {
                setPlace(place)
                return@launch
            }

            // The place may not have synced yet. Fall back to its coordinates so
            // the preview map still shows where it is, like a deep link does.
            val coordinates = try {
                withContext(Dispatchers.IO) { api().getPlaceCoordinates(requestedId) }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                null
            } ?: return@launch

            previewPlace = null
            previewLatLng = LatLng(coordinates.lat, coordinates.lon)
            renderSmallMap()
        }
    }

    /**
     * Re-reads the place being shown from the local cache and renders the new
     * copy. Does nothing before a place has been selected, and leaves the row
     * up if it disappeared from the cache (for example a deleted place), since
     * there is nothing fresher to show.
     */
    private fun reloadPlace() {
        val id = placeId
        if (id <= 0L) return

        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) { db().place.selectById(id) } ?: return@launch
            if (_binding == null) return@launch
            setPlace(place)
        }
    }

    private fun onSaveClicked() {
        if (prefs.authorized) {
            toggleSavedPlaceNow(placeId, placeName)
        } else {
            showAuthDialog(Bundle().apply {
                putString(EXTRA_AUTH_ACTION, AUTH_ACTION_TOGGLE_SAVED)
                putLong(EXTRA_PLACE_ID, placeId)
                putString(EXTRA_PLACE_NAME, placeName)
            })
        }
    }

    private fun toggleSavedPlaceNow(placeId: Long, placeName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                toggleSavedPlace(placeId, placeName)
                updateBookmarkIcon()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (smallMapCreated) _binding?.map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (smallMapCreated) _binding?.map?.onResume()
    }

    override fun onPause() {
        if (smallMapCreated) _binding?.map?.onPause()
        super.onPause()
    }

    override fun onStop() {
        if (smallMapCreated) _binding?.map?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (smallMapCreated) _binding?.map?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (smallMapCreated) _binding?.map?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The preview map owns native resources and only receives lifecycle
        // callbacks when this is a standalone screen; without onDestroy it
        // would leave a dead renderer behind.
        if (smallMapCreated) {
            _binding?.map?.onDestroy()
            smallMapCreated = false
        }
        smallMap = null
        _binding = null
    }

    fun setPlace(place: Place) {
        placeId = place.id
        placeName = place.getLocalizedName()

        binding.toolbar.title = place.getLocalizedName()
        binding.toolbar.setSingleLine(false)

        updateBookmarkIcon()

        if (place.requiredAppUrl != null) {
            binding.companionWarning.isVisible = true
            binding.companionWarning.setTextColor(requireContext().getErrorColor())
            binding.companionWarning.text =
                getString(R.string.companion_warning, place.requiredAppUrl.toString().trimEnd('/'))
        } else {
            binding.companionWarning.isVisible = false
        }

        if (place.verifiedAt != null) {
            val date = DateUtils.getRelativeDateTimeString(
                requireContext(),
                place.verifiedAt.toLocalDate().toEpochDay() * 24 * 3600 * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0,
            ).split(",").first()

            binding.lastVerified.text = date

            if (place.verifiedAt.isAfter(ZonedDateTime.now().minusYears(1))) {
                binding.lastVerified.isVisible = true
                binding.lastVerified.setTextColor(requireContext().getOnSurfaceColor())
                binding.lastVerified.setOnClickListener(null)
                binding.outdated.isVisible = false
                binding.outdated.setOnClickListener(null)
            } else {
                binding.lastVerified.isVisible = true
                binding.lastVerified.setTextColor(requireContext().getErrorColor())
                binding.lastVerified.setOnClickListener {
                    showVerificationWarning(R.string.verification_warning_outdated)
                }
                binding.outdated.isVisible = true
                binding.outdated.setOnClickListener {
                    showVerificationWarning(R.string.verification_warning_outdated)
                }
            }
        } else {
            binding.lastVerified.isVisible = true
            binding.lastVerified.text = getString(R.string.not_verified)
            binding.lastVerified.setTextColor(requireContext().getErrorColor())
            binding.lastVerified.setOnClickListener {
                showVerificationWarning(R.string.verification_warning_not_verified)
            }
            binding.outdated.isVisible = true
            binding.outdated.setOnClickListener {
                showVerificationWarning(R.string.verification_warning_not_verified)
            }
        }

        binding.address.text = place.address
        binding.address.isVisible = !place.address.isNullOrBlank()

        binding.phone.text = place.phone
        binding.phone.isVisible = !place.phone.isNullOrBlank()

        binding.website.text = place.website.toString().replace("https://", "").trimEnd('/')
        binding.website.isVisible = place.website != null

        if (place.twitter == null) {
            binding.twitter.isVisible = false
        } else {
            binding.twitter.isVisible = true
            binding.twitter.text =
                place.twitter.toString().replace("https://twitter.com/", "").trim('@')
            binding.twitter.styleAsLink()
            binding.twitter.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.twitter.toString().toUri()
                startActivity(intent)
            }
        }

        if (place.telegram == null) {
            binding.telegram.isVisible = false
        } else {
            binding.telegram.isVisible = true
            binding.telegram.text = place.telegram.toString().replace("https://t.me/", "")
            binding.telegram.styleAsLink()
            binding.telegram.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.telegram.toString().toUri()
                startActivity(intent)
            }
        }

        if (place.line == null) {
            binding.line.isVisible = false
        } else {
            binding.line.isVisible = true
            if (place.line.queryParameter("accountId").isNullOrBlank()) {
                binding.line.text = place.line.toString().replace("https://line.me/R/ti/p/@", "")
            } else {
                binding.line.text = place.line.queryParameter("accountId")
            }
            binding.line.styleAsLink()
            binding.line.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.line.toString().toUri()
                startActivity(intent)
            }
        }

        if (place.facebook == null) {
            binding.facebook.isVisible = false
        } else {
            binding.facebook.isVisible = true
            var text =
                place.facebook.toString().replace("https://www.facebook.com/people/", "")
                    .replace("https://www.facebook.com/p/", "")
                    .replace("https://www.facebook.com/", "")
                    .replace("https://facebook.com/", "").trimEnd('/')
            if (text.contains("/") && text.split("/").size == 2) {
                text = text.split("/").first()
            }
            binding.facebook.text = text
            binding.facebook.styleAsLink()
            binding.facebook.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.facebook.toString().toUri()
                startActivity(intent)
            }
        }

        if (place.instagram == null) {
            binding.instagram.isVisible = false
        } else {
            binding.instagram.isVisible = true
            binding.instagram.text =
                place.instagram.toString().replace("https://www.instagram.com/", "")
                    .replace("https://instagram.com/", "").trim('@', '/')
            binding.instagram.styleAsLink()
            binding.instagram.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.instagram.toString().toUri()
                startActivity(intent)
            }
        }

        binding.email.text = place.email
        binding.email.isVisible = place.email != null

        val openingHours = place.openingHours?.toOpeningHours()
        binding.openingHours.text = when {
            openingHours != null -> openingHours.toEmphasizedString(
                closedLabel = getString(R.string.opening_hours_closed),
                aroundTheClockLabel = getString(R.string.opening_hours_open_24_7),
            )

            else -> place.openingHours
        }
        binding.openingHours.isVisible = !place.openingHours.isNullOrBlank()

        binding.btnVerify.setOnClickListener {
            openReport(defaultType = "verified")
        }

        binding.btnReport.setOnClickListener {
            openReport(defaultType = null)
        }

        binding.comments.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val hasComments = withContext(Dispatchers.IO) {
                    db().comment.selectCountByPlaceId(place.id) > 0
                }

                if (hasComments) {
                    openComments()
                } else {
                    openAddComment()
                }
            }
        }

        binding.boost.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<BoostFragment>(
                    R.id.fragmentContainerView, null, Bundle().apply {
                        putLong("place_id", place.id)
                    }
                )
                addToBackStack(null)
            }
        }

        binding.addComment.setOnClickListener { openAddComment() }

        binding.comments.isEnabled = true
        renderComments(place.id)

        previewPlace = place
        previewLatLng = LatLng(place.lat, place.lon)
        renderSmallMap()
    }

    /**
     * The parsed week as a multi-line string with today's line bolded and
     * tinted, so the current day stands out. A week that collapses to a single
     * line has no day to emphasize and is returned as-is.
     */
    private fun OpeningHours.toEmphasizedString(
        closedLabel: String,
        aroundTheClockLabel: String,
    ): CharSequence {
        val display = toDisplayString(
            closedLabel = closedLabel,
            aroundTheClockLabel = aroundTheClockLabel,
        )
        val lineIndex = todayLineIndex(LocalDate.now().dayOfWeek) ?: return display

        val lines = display.split('\n')
        val start = lines.take(lineIndex).sumOf { it.length + 1 }
        val end = start + lines[lineIndex].length

        return SpannableString(display).apply {
            val flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            setSpan(StyleSpan(Typeface.BOLD), start, end, flags)
            setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(requireContext(), R.color.opening_hours_today),
                ),
                start,
                end,
                flags,
            )
        }
    }

    /**
     * Loads a place's comments off the main thread and shows them in the
     * preview list and the comments button.
     *
     * Comments are synced globally, so the local table can be much larger than
     * one place's comments; reading it on the UI thread would block the frame.
     * The preview query only reads the newest few, and the total is a separate
     * count so the button can still label the full number.
     */
    private fun renderComments(placeId: Long) {
        commentsJob?.cancel()
        commentsJob = viewLifecycleOwner.lifecycleScope.launch {
            val (comments, total) = withContext(Dispatchers.IO) {
                db().comment.selectByPlaceId(placeId, COMMENTS_PREVIEW_LIMIT) to
                    db().comment.selectCountByPlaceId(placeId)
            }

            val binding = _binding ?: return@launch

            binding.commentsTitle.text = getString(R.string.comments_d, total)
            binding.commentsTitle.isVisible = total > 0
            binding.comments.text = if (total == 0L) {
                getString(R.string.comment)
            } else {
                getString(R.string.comments_d, total)
            }
            // The top button opens the add screen when there is nothing to list,
            // so the bottom button would be a duplicate; it only appears once
            // there are comments to sit under.
            binding.addComment.isVisible = total > 0
            val formatter = commentDateFormatter()
            commentsAdapter.submitList(comments.map { it.toAdapterItem(formatter) })
        }
    }

    /**
     * Explains a verification warning. The dialog is informational only: the
     * Verify and Report buttons on the place itself are what the user acts on.
     */
    private fun showVerificationWarning(@StringRes message: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.verification_warning_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Opens the place on btcmap.org in a Custom Tab.
     *
     * The browser is targeted explicitly: a plain VIEW intent would be captured
     * by this app's own verified App Link for btcmap.org/merchant and reopen the
     * place instead of the site. If no Custom Tabs browser is available, fall
     * back to a chooser that excludes this app, for the same reason.
     */
    private fun openOnBtcmap() {
        val uri = "https://btcmap.org/merchant/$placeId".toUri()

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

    private fun openReport(defaultType: String?) {
        if (prefs.authorized) {
            navigateToReport(placeId, defaultType)
        } else {
            showAuthDialog(Bundle().apply {
                putString(EXTRA_AUTH_ACTION, AUTH_ACTION_OPEN_REPORT)
                putLong(EXTRA_PLACE_ID, placeId)
                if (defaultType != null) putString(EXTRA_REPORT_TYPE, defaultType)
            })
        }
    }

    private fun navigateToReport(placeId: Long, defaultType: String?) {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            val args = Bundle().apply {
                putLong("place_id", placeId)
                if (defaultType != null) putString("default_type", defaultType)
            }
            replace<ReportPlaceFragment>(R.id.fragmentContainerView, null, args)
            addToBackStack(null)
        }
    }

    private fun openComments() {
        val placeId = placeId
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<CommentsFragment>(
                R.id.fragmentContainerView, null, Bundle().apply { putLong("place_id", placeId) }
            )
            addToBackStack(null)
        }
    }

    private fun openAddComment() {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<AddCommentFragment>(
                R.id.fragmentContainerView, null, Bundle().apply { putLong("place_id", placeId) }
            )
            addToBackStack(null)
        }
    }

    fun onSlide(bottomSheetTop: Int) {
        val toolbar = _binding?.toolbar ?: return
        val statusBarTop = ViewCompat.getRootWindowInsets(toolbar)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        toolbar.updateLayoutParams<LinearLayout.LayoutParams> {
            topMargin = (statusBarTop - bottomSheetTop).coerceIn(0, statusBarTop)
        }
    }

    private fun TextView.styleAsLink() {
        setText(
            SpannableString(text).apply {
                setSpan(
                    URLSpan(""), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            },
            TextView.BufferType.SPANNABLE,
        )
    }

    fun Toolbar.setSingleLine(singleLine: Boolean) {
        for (i in 0..childCount) {
            val child = getChildAt(i)
            if (child is TextView) {
                child.isSingleLine = singleLine
            }
        }
    }

    private fun updateBookmarkIcon() {
        if (prefs.authorized) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val saved = isPlaceSaved(placeId)
                    withResumed {
                        binding.toolbar.menu.findItem(R.id.save).setIcon(
                            if (saved) R.drawable.icon_bookmark_check else R.drawable.icon_bookmark
                        )
                    }
                } catch (e: Throwable) {
                    e.rethrowIfCancellation()
                    showError(e)
                }
            }
        } else {
            binding.toolbar.menu.findItem(R.id.save).setIcon(R.drawable.icon_bookmark)
        }
    }
}
