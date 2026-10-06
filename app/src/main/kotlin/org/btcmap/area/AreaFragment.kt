package org.btcmap.area

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import coil3.load
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.GetEventsItem
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
import org.btcmap.db.table.area.Area
import org.btcmap.databinding.AreaFragmentBinding
import org.btcmap.event.EventFragment
import org.btcmap.event.toBundle
import org.btcmap.i18n.getLocalizedDescription
import org.btcmap.i18n.getLocalizedName
import org.btcmap.saved.isAreaSaved
import org.btcmap.saved.toggleSavedArea
import org.btcmap.settings.authorized
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.prefs
import org.btcmap.sync.SyncEvent
import org.btcmap.syncController
import org.btcmap.util.iconTypeface
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError

/**
 * The area screen: a collapsing Views toolbar over the shared
 * [org.btcmap.ui.AreaScreen] body, which renders the description, the website,
 * the offline map panel and the boosted merchant, event and place issue
 * sections. This fragment owns the toolbar and loads the area and its sections
 * through the shared [AreaSections].
 */
class AreaFragment : Fragment() {

    private val areaId by lazy {
        val arguments = requireArguments()
        require(arguments.containsKey(ARG_AREA_ID)) { "Missing $ARG_AREA_ID argument" }
        arguments.getLong(ARG_AREA_ID)
    }

    private var _binding: AreaFragmentBinding? = null
    private val binding get() = _binding!!

    private var toolbarContentColor = Color.WHITE

    private var toolbarColorApplied = false

    private var appBarCollapsed = false

    private var areaName = ""

    /** The rendered area, kept so a sync-triggered refresh can re-read the cache. */
    private var loadedArea: Area? = null

    private var offlineMap: AreaOfflineMapController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AreaFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        offlineMap = AreaOfflineMapController(this, binding)

        // Events are read from the local cache, so a background sync that
        // changes them makes this section stale; re-render when it does instead
        // of leaving the stale list up until the screen is reopened.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncController().events.collect { event ->
                    if (event == SyncEvent.EventsChanged) {
                        loadedArea?.let { loadEvents(it, reportErrors = false) }
                    }
                }
            }
        }

        // Registered here, not when the dialog is shown, so a form that was open
        // when the device rotated still reaches this (recreated) fragment.
        registerAuthResultListener { extras ->
            if (extras.getString(EXTRA_AUTH_ACTION) == AUTH_ACTION_TOGGLE_SAVED) {
                toggleSavedAreaNow(
                    areaId = extras.getLong(EXTRA_AREA_ID, areaId),
                    areaName = extras.getString(EXTRA_AREA_NAME) ?: areaName,
                )
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.download -> offlineMap?.onDownloadClicked()
                R.id.save -> onSaveClicked()
            }

            true
        }

        binding.toolbar.menu.findItem(R.id.save).isEnabled = false
        binding.toolbar.menu.findItem(R.id.download).isVisible = false

        setUpContent()
        initInsets()
        initCollapsingToolbar()
        loadArea()
    }

    /** Hands the shared body everything it needs from this host. */
    private fun setUpContent() {
        val typeface = iconTypeface

        binding.areaContent.apply {
            strings = requireContext().areaStrings()
            boostedMarkerColor = ComposeColor(prefs.boostedMarkerBackgroundColor())
            iconTypeface = typeface
            onOpenPlace = { placeId -> (activity as? Activity)?.openPlace(placeId) }
            onOpenEvent = { event -> navigateToEvent(event) }
            onOpenIssue = { issue -> openIssue(issue) }
            onJoinUs = { openInBrowser(JOIN_US_URL.toUri()) }
        }
    }

    private fun loadArea() {
        viewLifecycleOwner.lifecycleScope.launch {
            // The area is read from the local cache: the screen is only opened
            // from a map chip or a search result, both of which come from the
            // area sync, so the row is present and the screen works offline.
            val area = withContext(Dispatchers.IO) { db().area.selectById(areaId) }
            if (area == null) {
                binding.loading.isVisible = false
                showLoadError()
                return@launch
            }

            loadedArea = area
            renderArea(area)
            offlineMap?.bind(area)
            binding.loading.isVisible = false
            binding.content.isVisible = true
        }
    }

    private fun renderArea(area: Area) {
        areaName = area.getLocalizedName()
        binding.toolbar.title = areaName

        val headerImage = area.iconWide ?: area.icon
        binding.icon.isVisible = headerImage != null
        binding.icon.load(headerImage)
        updateToolbarContentColor()

        binding.areaContent.apply {
            description = area.getLocalizedDescription()
            websiteText = websiteDisplayText(area.websiteUrl)
        }

        binding.toolbar.menu.findItem(R.id.save).isEnabled = true
        updateBookmarkIcon()

        loadBoostedMerchants(area)
        loadEvents(area)
        loadPlaceIssues(area.id)
    }

    private fun loadBoostedMerchants(area: Area) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val places = withContext(Dispatchers.IO) { AreaSections.boostedMerchants(db(), area) }
                _binding?.areaContent?.boostedMerchants = places
            } catch (e: Throwable) {
                // The section is secondary: when it cannot be read, leave it
                // hidden rather than interrupting the area screen.
                e.rethrowIfCancellation()
            }
        }
    }

    /**
     * Loads the area's upcoming events. [reportErrors] is false when re-running
     * after a background sync: a refresh that fails must not interrupt the screen
     * with an error dialog the user did not ask for.
     */
    private fun loadEvents(area: Area, reportErrors: Boolean = true) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) { AreaSections.events(db(), area) }
                _binding?.areaContent?.events = events
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                if (reportErrors) showError(e)
            }
        }
    }

    private fun loadPlaceIssues(areaId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val issues = withContext(Dispatchers.IO) {
                    AreaSections.placeIssues(api(), db(), areaId)
                }
                _binding?.areaContent?.issues = issues
            } catch (e: Throwable) {
                // The issues list is secondary: when it cannot be fetched
                // (typically offline) leave the section hidden rather than
                // interrupting the area screen, which is fully usable offline.
                e.rethrowIfCancellation()
            }
        }
    }

    private fun onSaveClicked() {
        if (prefs.authorized) {
            toggleSavedAreaNow(areaId, areaName)
        } else {
            showAuthDialog(Bundle().apply {
                putString(EXTRA_AUTH_ACTION, AUTH_ACTION_TOGGLE_SAVED)
                putLong(EXTRA_AREA_ID, areaId)
                putString(EXTRA_AREA_NAME, areaName)
            })
        }
    }

    private fun toggleSavedAreaNow(areaId: Long, areaName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                toggleSavedArea(areaId, areaName)
                updateBookmarkIcon()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun navigateToEvent(event: GetEventsItem) {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<EventFragment>(R.id.fragmentContainerView, null, event.toBundle())
            addToBackStack(null)
        }
    }

    private fun openIssue(issue: AreaPlaceIssue) {
        openInBrowser(
            "https://www.openstreetmap.org/edit?${issue.elementOsmType}=${issue.elementOsmId}"
                .toUri(),
        )
    }

    private fun showLoadError() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error)
            .setMessage(R.string.error)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setOnCancelListener {
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        updateSystemBarAppearance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !isNightMode()
        }
        offlineMap = null
        loadedArea = null
        _binding = null
    }

    private fun initInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val top = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.toolbar.updatePadding(top = top)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun initCollapsingToolbar() {
        binding.appBar.addOnOffsetChangedListener { _, verticalOffset ->
            appBarCollapsed = verticalOffset <= -binding.appBar.totalScrollRange
            updateToolbarContentColor()
        }
        updateToolbarContentColor()
    }

    private fun updateToolbarContentColor() {
        val toolbar = binding.toolbar
        val color = if (showOverImage()) {
            Color.WHITE
        } else {
            MaterialColors.getColor(
                toolbar,
                com.google.android.material.R.attr.colorOnSurface,
                Color.BLACK,
            )
        }
        if (toolbarColorApplied && color == toolbarContentColor) return
        toolbarColorApplied = true
        toolbarContentColor = color
        toolbar.setTitleTextColor(color)
        toolbar.navigationIcon?.mutate()?.setTint(color)
        toolbar.menu.findItem(R.id.download)?.iconTintList = ColorStateList.valueOf(color)
        toolbar.menu.findItem(R.id.save)?.iconTintList = ColorStateList.valueOf(color)
        updateSystemBarAppearance()
    }

    private fun showOverImage(): Boolean = binding.icon.isVisible && !appBarCollapsed

    private fun updateSystemBarAppearance() {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !showOverImage() && !isNightMode()
        }
    }

    private fun isNightMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateBookmarkIcon() {
        if (!prefs.authorized) {
            binding.toolbar.menu.findItem(R.id.save).setIcon(R.drawable.icon_bookmark)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { isAreaSaved(areaId) }
                withResumed {
                    binding.toolbar.menu.findItem(R.id.save).apply {
                        setIcon(
                            if (saved) R.drawable.icon_bookmark_check else R.drawable.icon_bookmark
                        )
                        iconTintList = ColorStateList.valueOf(toolbarContentColor)
                    }
                }
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    companion object {
        private const val JOIN_US_URL = "https://btcmap.org/join-us"

        private const val EXTRA_AUTH_ACTION = "auth-action"
        private const val EXTRA_AREA_ID = "auth-area-id"
        private const val EXTRA_AREA_NAME = "auth-area-name"

        private const val AUTH_ACTION_TOGGLE_SAVED = "toggle-saved"
    }
}
