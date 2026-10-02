package org.btcmap.place

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.Color
import androidx.appcompat.widget.Toolbar
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
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.Activity
import org.btcmap.api
import org.btcmap.api.getPlaceCoordinates
import org.btcmap.api.getPlaceImages
import org.btcmap.api.placeImageUrl
import org.btcmap.app
import org.btcmap.boost.BoostFragment
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.place.toMarker
import org.btcmap.map.markerImageName
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
import org.btcmap.sync.SyncEvent
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
import org.btcmap.settings.boostedMarkerIconColor
import org.btcmap.settings.mapStyle
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.markerIconColor
import org.btcmap.settings.uri
import org.btcmap.syncController
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation
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
        private const val AUTH_ACTION_ADD_PHOTO = "add-photo"

        /**
         * How many comments the place screen previews inline. A place's full
         * list is unbounded, so the preview query is capped here to keep the
         * sheet's RecyclerView from inflating with every comment; the comments
         * button shows the total and opens them all.
         */
        private const val COMMENTS_PREVIEW_LIMIT = 3L

        /** The thumbnail strip asks the API for small, square renditions. */
        private const val PHOTO_THUMBNAIL_SIZE = 320

        /** The fullscreen viewer asks for a larger rendition of the same photo. */
        private const val PHOTO_FULL_SIZE = 1600


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

    private lateinit var photosAdapter: PlacePhotosAdapter

    private var photosJob: Job? = null

    /** The place whose photos the strip currently belongs to, or null. */
    private var renderedPhotosPlaceId: Long? = null

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
        onUploaded = { placeId -> renderPhotos(placeId) },
        onError = { showError(it) },
    )

    private var previewPlace: Place? = null
    private var previewLat: Double? = null
    private var previewLon: Double? = null

    /** The place's OpenStreetMap page, or null when it has no OSM id. */
    private var osmUrl: String? = null

    /** The place's OpenStreetMap editor page, or null when it has no OSM id. */
    private var osmEditUrl: String? = null

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
                    placeName = extras.getString(EXTRA_PLACE_NAME) ?: placeName,
                    defaultType = extras.getString(EXTRA_REPORT_TYPE),
                )

                AUTH_ACTION_ADD_PHOTO -> requestAddPhoto()
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

                R.id.view_on_osm -> openOnOpenStreetMap()

                R.id.edit_on_osm -> openOsmEditor()

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

        photosAdapter = PlacePhotosAdapter { index -> showPhotoViewer(index) }
        binding.photosList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.photosList.adapter = ConcatAdapter(
            photosAdapter,
            PlacePhotoAddAdapter { requestAddPhoto() },
        )

        binding.addPhoto.setOnClickListener { requestAddPhoto() }

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
        renderSmallMap()
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


    private fun styleUri(): String {
        return app().mapStyleUriForTesting ?: prefs.mapStyle.uri(requireContext())
    }


    /** Points the preview map at the place, with its marker when it is known. */
    private fun renderSmallMap() {
        val binding = _binding ?: return
        val lat = previewLat ?: return
        val lon = previewLon ?: return

        binding.map.apply {
            this.lat = lat
            this.lon = lon
            marker = previewPlace?.toMarker()
            styleUrl = styleUri()
            markerBackgroundColor = Color(prefs.markerBackgroundColor(requireContext()))
            markerIconColor = Color(prefs.markerIconColor(requireContext()))
            boostedMarkerBackgroundColor = Color(prefs.boostedMarkerBackgroundColor())
            boostedMarkerIconColor = Color(prefs.boostedMarkerIconColor())
            markerBadgeBackgroundColor = Color(prefs.badgeBackgroundColor(requireContext()))
            markerBadgeTextColor = Color(prefs.badgeTextColor(requireContext()))
            usingOpenFreeMap = app().mapStyleUriForTesting == null
            iconTypeface = org.btcmap.util.iconTypeface
            onClick = { (activity as? Activity)?.openPlace(requestedPlaceId) }
        }
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
            previewLat = coordinates.lat
            previewLon = coordinates.lon
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
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        photoUploader.hideUploading()
        _binding = null
    }

    fun setPlace(place: Place) {
        placeId = place.id
        placeName = place.getLocalizedName()

        binding.toolbar.title = place.getLocalizedName()
        binding.toolbar.setSingleLine(false)

        osmUrl = place.osmUrl()
        binding.toolbar.menu.findItem(R.id.view_on_osm)?.isVisible = osmUrl != null
        osmEditUrl = place.osmEditUrl()
        binding.toolbar.menu.findItem(R.id.edit_on_osm)?.isVisible = osmEditUrl != null

        updateBookmarkIcon()

        if (place.requiredAppUrl != null) {
            binding.companionWarning.isVisible = true
            binding.companionWarning.setTextColor(requireContext().getErrorColor())
            binding.companionWarning.text =
                getString(R.string.companion_warning, place.requiredAppUrl.toString().trimEnd('/'))
        } else {
            binding.companionWarning.isVisible = false
        }

        val verifiedAt = place.verifiedAt
        if (verifiedAt != null) {
            val date = DateUtils.getRelativeDateTimeString(
                requireContext(),
                verifiedAt.toLocalDate().toEpochDay() * 24 * 3600 * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0,
            ).split(",").first()

            binding.lastVerified.text = date

            if (verifiedAt.isAfter(ZonedDateTime.now().minusYears(1))) {
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

        val line = place.line
        if (line == null) {
            binding.line.isVisible = false
        } else {
            binding.line.isVisible = true
            if (line.queryParameter("accountId").isNullOrBlank()) {
                binding.line.text = line.toString().replace("https://line.me/R/ti/p/@", "")
            } else {
                binding.line.text = line.queryParameter("accountId")
            }
            binding.line.styleAsLink()
            binding.line.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = line.toString().toUri()
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
                        putString("place_name", placeName)
                    }
                )
                addToBackStack(null)
            }
        }

        binding.addComment.setOnClickListener { openAddComment() }

        binding.comments.isEnabled = true
        renderComments(place.id)
        renderPhotos(place.id)

        previewPlace = place
        previewLat = place.lat
        previewLon = place.lon
        renderSmallMap()
    }

    /**
     * The parsed week as a multi-line string with today's line underlined, so
     * the current day stands out. A week that collapses to a single line has no
     * day to emphasize and is returned as-is.
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
            setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
     * Loads a place's photos off the main thread and shows them in the thumbnail
     * strip. Most places have none, so the whole section stays hidden until at
     * least one image arrives; a failed fetch leaves it hidden rather than
     * interrupting the screen.
     */
    private fun renderPhotos(placeId: Long) {
        photosJob?.cancel()

        // Switching to a different place must not leave the previous place's
        // photos on screen, even for a frame, so hide the whole section until
        // the new place's list is committed. A reload of the same place keeps
        // its strip up to avoid flicker.
        if (renderedPhotosPlaceId != placeId) {
            renderedPhotosPlaceId = placeId
            _binding?.let {
                it.photosList.isVisible = false
                it.addPhoto.isVisible = false
            }
        }

        photosJob = viewLifecycleOwner.lifecycleScope.launch {
            val api = api()

            val photos = try {
                withContext(Dispatchers.IO) {
                    api.getPlaceImages(placeId).map { image ->
                        PlacePhoto(
                            id = image.id,
                            thumbnailUrl = api.placeImageUrl(
                                placeId = placeId,
                                imageId = image.id,
                                width = PHOTO_THUMBNAIL_SIZE,
                                height = PHOTO_THUMBNAIL_SIZE,
                            ),
                            fullUrl = api.placeImageUrl(
                                placeId = placeId,
                                imageId = image.id,
                                width = PHOTO_FULL_SIZE,
                            ),
                        )
                    }
                }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                emptyList()
            }

            if (_binding == null) return@launch

            // With photos, the strip ends in an add tile; without any, the same
            // spot shows a full-width add button so the affordance stays there.
            // Reveal only once the new list is committed, so the previous
            // place's rows cannot flash while the diff is applied.
            photosAdapter.submitList(photos) {
                val binding = _binding
                if (binding != null) {
                    val hasPhotos = photos.isNotEmpty()
                    val showList = hasPhotos
                    val showAdd = !hasPhotos
                    val wasListVisible = binding.photosList.isVisible

                    if (wasListVisible != showList ||
                        binding.addPhoto.isVisible != showAdd
                    ) {
                        // Animate the row in and let the rows below slide down
                        // instead of jumping when the network result lands.
                        (binding.photosList.parent as? ViewGroup)?.let {
                            TransitionManager.beginDelayedTransition(it)
                        }
                        binding.photosList.isVisible = showList
                        binding.addPhoto.isVisible = showAdd

                        // The list was committed while hidden, so pin it to the
                        // first photo; otherwise the layout manager can settle
                        // on the last one and open the strip scrolled to the end.
                        if (showList && !wasListVisible) {
                            binding.photosList.scrollToPosition(0)
                        }
                    }
                }
            }
        }
    }

    private fun showPhotoViewer(index: Int) {
        val urls = photosAdapter.currentList.map { it.fullUrl }
        if (urls.isEmpty()) return

        PlacePhotoViewerDialogFragment.newInstance(ArrayList(urls), index)
            .show(parentFragmentManager, PlacePhotoViewerDialogFragment.TAG)
    }

    /** Uploads require a signed-in user, so prompt for auth first if needed. */
    private fun requestAddPhoto() {
        photoUploader.request(placeId, placeName)
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

    /**
     * Opens the place's OpenStreetMap element page. The action is hidden for a
     * place without an OSM id, so there is always a URL by the time this runs.
     */
    private fun openOnOpenStreetMap() {
        val url = osmUrl ?: return
        openInBrowser(url.toUri())
    }

    /**
     * Opens the place in the OpenStreetMap web editor with the element selected.
     * Hidden without an OSM id, so there is always a URL by the time this runs.
     */
    private fun openOsmEditor() {
        val url = osmEditUrl ?: return
        openInBrowser(url.toUri())
    }

    private fun openReport(defaultType: String?) {
        if (prefs.authorized) {
            navigateToReport(placeId, placeName, defaultType)
        } else {
            showAuthDialog(Bundle().apply {
                putString(EXTRA_AUTH_ACTION, AUTH_ACTION_OPEN_REPORT)
                putLong(EXTRA_PLACE_ID, placeId)
                putString(EXTRA_PLACE_NAME, placeName)
                if (defaultType != null) putString(EXTRA_REPORT_TYPE, defaultType)
            })
        }
    }

    private fun navigateToReport(placeId: Long, placeName: String, defaultType: String?) {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            val args = Bundle().apply {
                putLong("place_id", placeId)
                putString("place_name", placeName)
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
                R.id.fragmentContainerView,
                null,
                Bundle().apply {
                    putLong("place_id", placeId)
                    putString("place_name", placeName)
                },
            )
            addToBackStack(null)
        }
    }

    private fun openAddComment() {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<AddCommentFragment>(
                R.id.fragmentContainerView,
                null,
                Bundle().apply {
                    putLong("place_id", placeId)
                    putString("place_name", placeName)
                },
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
