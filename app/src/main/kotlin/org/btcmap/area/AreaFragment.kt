package org.btcmap.area

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
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
import org.btcmap.R
import org.btcmap.SyncEvent
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
import org.btcmap.db.table.area.Area
import org.btcmap.databinding.AreaFragmentBinding
import org.btcmap.saved.isAreaSaved
import org.btcmap.saved.toggleSavedArea
import org.btcmap.settings.authorized
import org.btcmap.settings.prefs
import org.btcmap.syncController
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError

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

    private var sections: AreaSectionsController? = null

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
        sections = AreaSectionsController(this, binding)

        // Events are read from the local cache, so a background sync that
        // changes them makes this section stale; re-render when it does instead
        // of leaving the stale list up until the screen is reopened.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncController().events.collect { event ->
                    if (event == SyncEvent.EventsChanged) {
                        loadedArea?.let { sections?.loadEvents(it, reportErrors = false) }
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

        binding.issuesHelp.setOnClickListener {
            openInBrowser(JOIN_US_URL.toUri())
        }

        initInsets()
        initCollapsingToolbar()
        loadArea()
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

            sections?.loadBoostedMerchants(area)
            sections?.loadEvents(area)
            sections?.loadPlaceIssues(areaId)
        }
    }

    private fun renderArea(area: Area) {
        areaName = area.name
        binding.toolbar.title = area.name
        val headerImage = area.iconWide ?: area.icon
        binding.icon.isVisible = headerImage != null
        binding.icon.load(headerImage)
        updateToolbarContentColor()
        renderDescription(area.description)
        binding.website.text = websiteDisplayText(area.websiteUrl)
        binding.toolbar.menu.findItem(R.id.save).isEnabled = true
        updateBookmarkIcon()
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

    private fun renderDescription(description: String?) {
        val paragraphs = descriptionParagraphs(description)
        binding.description.isVisible = description != null

        if (paragraphs.size <= 1) {
            binding.description.text = description
            binding.descriptionExpand.isVisible = false
            return
        }

        var expanded = false
        binding.description.text = paragraphs.first()
        binding.descriptionExpand.isVisible = true
        binding.descriptionExpand.setText(R.string.read_more)
        binding.descriptionExpand.setOnClickListener {
            expanded = !expanded
            binding.description.text =
                if (expanded) paragraphs.joinToString("\n\n") else paragraphs.first()
            binding.descriptionExpand.setText(
                if (expanded) R.string.collapse else R.string.read_more
            )
        }
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
        sections = null
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
