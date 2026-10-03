package org.btcmap.place

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.getPlaceImages
import org.btcmap.api.placeImageUrl
import org.btcmap.app
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.boost.BoostFragment
import org.btcmap.comment.AddCommentFragment
import org.btcmap.comment.commentDateFormatter
import org.btcmap.comment.toAdapterItem
import org.btcmap.db
import org.btcmap.db.table.place.Place
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
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.btcmap.sync.SyncEvent
import org.btcmap.syncController
import org.btcmap.ui.PlaceAction
import org.btcmap.util.iconTypeface
import org.btcmap.util.openDialer
import org.btcmap.util.openEmail
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError

/**
 * The standalone place screen, opened outside the map (today from the activity
 * feed): a Views top bar over the shared [org.btcmap.ui.PlaceDetails] body, which
 * renders the preview map, the contact details, the photos, the comments and the
 * actions. This fragment loads the place and routes the actions the Compose body
 * and the toolbar raise.
 */
class PlaceFragment : Fragment() {

    companion object {
        /**
         * Marks a standalone place screen: it carries the place to show when the
         * screen was not handed one directly.
         */
        const val ARG_PLACE_ID = "place_id"

        private const val EXTRA_AUTH_ACTION = "auth-action"
        private const val EXTRA_PLACE_ID = "auth-place-id"
        private const val EXTRA_PLACE_NAME = "auth-place-name"
        private const val EXTRA_REPORT_TYPE = "auth-report-type"

        private const val AUTH_ACTION_TOGGLE_SAVED = "toggle-saved"
        private const val AUTH_ACTION_OPEN_REPORT = "open-report"
        private const val AUTH_ACTION_ADD_PHOTO = "add-photo"

        /** The thumbnail strip asks the API for small, square renditions. */
        private const val PHOTO_SIZE = 320

        /** A place screen from a place id, e.g. a tapped activity-feed row. */
        fun create(placeId: Long): PlaceFragment {
            return PlaceFragment().apply {
                arguments = Bundle().apply { putLong(ARG_PLACE_ID, placeId) }
            }
        }
    }

    private var placeId = 0L

    private var placeName = ""

    /** The place's OpenStreetMap page, or null when it has no OSM id. */
    private var osmUrl: String? = null

    /** The rendered place, kept so its contact rows can open their links. */
    private var place: Place? = null

    /** The place's OpenStreetMap editor page, or null when it has no OSM id. */
    private var osmEditUrl: String? = null

    private val requestedPlaceId: Long
        get() = arguments?.getLong(ARG_PLACE_ID, 0L) ?: 0L

    private var _binding: PlaceFragmentBinding? = null
    private val binding get() = _binding!!

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.directions -> openDirections()
                R.id.share -> share()
                R.id.view_on_btcmap -> openOnBtcmap()
                R.id.view_on_osm -> openOnOpenStreetMap()
                R.id.edit_on_osm -> openOsmEditor()
                R.id.save -> onSaveClicked()
            }
            true
        }

        setUpContent()

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

        loadPlace()
    }

    /** Hands the shared body everything it needs from this host. */
    private fun setUpContent() {
        val styleUrl = app().mapStyleUriForTesting ?: prefs.mapStyle.uri(requireContext())
        // Read before apply(): inside it, the name would resolve to the view's
        // own property instead of the icon font built from the assets.
        val typeface = iconTypeface

        binding.placeContent.apply {
            this.styleUrl = styleUrl
            usingOpenFreeMap = app().mapStyleUriForTesting == null
            markerBackgroundColor = Color(prefs.markerBackgroundColor(requireContext()))
            markerIconColor = Color(prefs.markerIconColor(requireContext()))
            boostedMarkerBackgroundColor = Color(prefs.boostedMarkerBackgroundColor())
            boostedMarkerIconColor = Color(prefs.boostedMarkerIconColor())
            markerBadgeBackgroundColor = Color(prefs.badgeBackgroundColor(requireContext()))
            markerBadgeTextColor = Color(prefs.badgeTextColor(requireContext()))
            iconTypeface = typeface
            strings = requireContext().placeSheetStrings()
            onAction = { action -> onPlaceAction(action) }
            onPreviewMapClick = { (activity as? Activity)?.openPlace(requestedPlaceId) }
        }
    }

    private fun loadPlace() {
        val requestedId = requestedPlaceId
        if (requestedId <= 0L) return

        viewLifecycleOwner.lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) { db().place.selectById(requestedId) }
                ?: return@launch
            if (_binding == null) return@launch
            setPlace(place)
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

    fun setPlace(place: Place) {
        placeId = place.id
        placeName = place.getLocalizedName()

        binding.toolbar.title = placeName
        binding.toolbar.setSingleLine(false)

        osmUrl = place.osmUrl()
        binding.toolbar.menu.findItem(R.id.view_on_osm)?.isVisible = osmUrl != null
        osmEditUrl = place.osmEditUrl()
        binding.toolbar.menu.findItem(R.id.edit_on_osm)?.isVisible = osmEditUrl != null
        this.place = place

        binding.placeContent.place = place

        updateBookmarkIcon()
        renderComments(place.id)
        renderPhotos(place.id)
    }

    /**
     * Loads a place's comments off the main thread and shows them in the body.
     * Comments are synced globally, so the local table can be much larger than
     * one place's comments; reading it on the UI thread would block the frame.
     */
    private fun renderComments(placeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                val formatter = commentDateFormatter()
                db().comment.selectByPlaceId(placeId).map { it.toAdapterItem(formatter) }
            }
            _binding?.placeContent?.comments = items
        }
    }

    /**
     * Loads a place's photos off the main thread and shows them in the
     * thumbnail strip. Most places have none; a failed fetch leaves the section
     * empty rather than interrupting the screen.
     */
    private fun renderPhotos(placeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val photos = try {
                withContext(Dispatchers.IO) {
                    api().getPlaceImages(placeId).map { image ->
                        api().placeImageUrl(
                            placeId = placeId,
                            imageId = image.id,
                            width = PHOTO_SIZE,
                            height = PHOTO_SIZE,
                        )
                    }
                }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                emptyList()
            }

            _binding?.placeContent?.photos = photos
        }
    }

    private fun onPlaceAction(action: PlaceAction) {
        when (action) {
            PlaceAction.Directions -> openDirections()
            PlaceAction.Verify -> openReport(defaultType = "verified")
            PlaceAction.Report -> openReport(defaultType = null)
            PlaceAction.Boost -> openBoost()
            PlaceAction.AddComment -> openAddComment()
            PlaceAction.AddPhoto -> requestAddPhoto()
            PlaceAction.Phone -> openDialer(place?.phone)
            PlaceAction.Website -> place?.website?.let { openInBrowser(it.toString().toUri()) }
            PlaceAction.Email -> openEmail(place?.email)
            PlaceAction.Telegram -> place?.telegram?.let { openInBrowser(it.toString().toUri()) }
            PlaceAction.Line -> place?.line?.let { openInBrowser(it.toString().toUri()) }
            PlaceAction.Twitter -> place?.twitter?.let { openInBrowser(it.toString().toUri()) }
            PlaceAction.Facebook -> place?.facebook?.let { openInBrowser(it.toString().toUri()) }
            PlaceAction.Instagram -> place?.instagram?.let { openInBrowser(it.toString().toUri()) }
            // The rest are raised by the top bar, not the body.
            else -> {}
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

    private fun updateBookmarkIcon() {
        if (prefs.authorized) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val saved = isPlaceSaved(placeId)
                    withResumed {
                        _binding?.placeContent?.bookmarked = saved
                        binding.toolbar.menu.findItem(R.id.save).setIcon(
                            if (saved) R.drawable.icon_bookmark_check else R.drawable.icon_bookmark,
                        )
                    }
                } catch (e: Throwable) {
                    e.rethrowIfCancellation()
                    showError(e)
                }
            }
        } else {
            _binding?.placeContent?.bookmarked = false
            binding.toolbar.menu.findItem(R.id.save).setIcon(R.drawable.icon_bookmark)
        }
    }

    /** Uploads require a signed-in user, so prompt for auth first if needed. */
    private fun requestAddPhoto() {
        photoUploader.request(placeId, placeName)
    }

    private fun openBoost() {
        navigate(
            BoostFragment(),
            Bundle().apply {
                putLong("place_id", placeId)
                putString("place_name", placeName)
            },
        )
    }

    private fun openAddComment() {
        navigate(
            AddCommentFragment(),
            Bundle().apply {
                putLong("place_id", placeId)
                putString("place_name", placeName)
            },
        )
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
        navigate(
            ReportPlaceFragment(),
            Bundle().apply {
                putLong("place_id", placeId)
                putString("place_name", placeName)
                if (defaultType != null) putString("default_type", defaultType)
            },
        )
    }

    private fun navigate(fragment: Fragment, args: Bundle?) {
        fragment.arguments = args
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainerView, fragment)
            addToBackStack(null)
        }
    }

    private fun openDirections() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Read the coordinates off the main thread: the shared connection's
            // lock can stall a read behind a background sync write, and this runs
            // on the main thread.
            val place = withContext(Dispatchers.IO) { db().place.selectById(placeId) }
                ?: return@launch
            val uri = "geo:${place.lat},${place.lon}?q=${place.getLocalizedName()}".toUri()
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), null))
        }
    }

    private fun share() {
        val uri = "https://btcmap.org/merchant/$placeId".toUri()
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, uri.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, null))
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

    override fun onDestroyView() {
        super.onDestroyView()
        photoUploader.hideUploading()
        _binding = null
    }
}

/** Lets the toolbar title wrap instead of truncating a long place name. */
private fun Toolbar.setSingleLine(singleLine: Boolean) {
    for (i in 0..childCount) {
        val child = getChildAt(i)
        if (child is TextView) {
            child.isSingleLine = singleLine
        }
    }
}
