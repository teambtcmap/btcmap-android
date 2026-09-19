package org.btcmap.area

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import coil3.load
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.GetAreaItem
import org.btcmap.api.GetEventsItem
import org.btcmap.api.GetPlaceIssuesItem
import org.btcmap.api.getPlaceIssues
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.area.geoJsonGeometry
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.databinding.AreaFragmentBinding
import org.btcmap.databinding.ItemAreaCardBinding
import org.btcmap.event.EventFragment
import org.btcmap.event.toBundle
import org.btcmap.saved.isAreaSaved
import org.btcmap.saved.toggleSavedArea
import org.btcmap.settings.authorized
import org.btcmap.settings.prefs
import org.btcmap.util.iconTypeface
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

    private val eventDateFormat: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

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
                R.id.save -> onSaveClicked()
            }

            true
        }

        binding.toolbar.menu.findItem(R.id.save).isEnabled = false

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
                showLoadError(IllegalStateException())
                return@launch
            }

            renderArea(area.toGetAreaItem())
            binding.loading.isVisible = false
            binding.content.isVisible = true

            loadEvents()
            loadPlaceIssues()
        }
    }

    private fun Area.toGetAreaItem(): GetAreaItem {
        return GetAreaItem(
            id = id,
            name = name,
            type = type,
            urlAlias = urlAlias,
            icon = icon,
            iconWide = iconWide,
            websiteUrl = websiteUrl,
            description = description,
        )
    }

    private fun renderArea(area: GetAreaItem) {
        areaName = area.name
        binding.toolbar.title = area.name
        val headerImage = area.iconWide ?: area.icon
        binding.icon.isVisible = headerImage != null
        binding.icon.load(headerImage)
        updateToolbarContentColor()
        renderDescription(area.description)
        binding.website.text = websiteDisplayText(area.id, area.websiteUrl)
        binding.toolbar.menu.findItem(R.id.save).isEnabled = true
        updateBookmarkIcon()
    }

    private fun loadEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) { fetchAreaEvents() }
                renderUpcomingEvents(events)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun loadPlaceIssues() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (rows, totalIssues) = withContext(Dispatchers.IO) {
                    fetchPlaceIssues()
                }
                renderPlaceIssues(rows, totalIssues)
            } catch (e: Throwable) {
                // The issues list is secondary: when it cannot be fetched
                // (typically offline) leave the section hidden rather than
                // interrupting the area screen, which is fully usable offline.
                e.rethrowIfCancellation()
            }
        }
    }

    /**
     * Reads the area's upcoming events from the local cache instead of calling
     * `GET /v4/areas/{id}/events`.
     *
     * The v4 events payload does not carry `area_id`, so the association cannot
     * be looked up by column. The server links an event to an area by
     * pre-filtering on the area's bbox and then running a point-in-polygon test
     * against its GeoJSON; the same rule is applied here to the cached area
     * geometry, mirroring [org.btcmap.map.MapAreasController].
     */
    private fun fetchAreaEvents(): List<GetEventsItem> {
        val area = db().area.selectById(areaId) ?: return emptyList()
        val west = area.bboxWest ?: return emptyList()
        val south = area.bboxSouth ?: return emptyList()
        val east = area.bboxEast ?: return emptyList()
        val north = area.bboxNorth ?: return emptyList()

        val geometry = area.geoJsonGeometry()
        return db().event.selectByBounds(south, north, west, east)
            .filter { geometry.contains(it.lat, it.lon) }
            .map { it.toGetEventsItem() }
    }

    private fun Event.toGetEventsItem(): GetEventsItem {
        return GetEventsItem(
            id = id,
            areaId = areaId,
            lat = lat,
            lon = lon,
            name = name,
            website = website,
            startsAt = startsAt,
            endsAt = endsAt,
        )
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

    private fun showLoadError(throwable: Throwable) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error)
            .setMessage(throwable.message?.takeIf { it.isNotBlank() } ?: getString(R.string.error))
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

    private fun renderUpcomingEvents(events: List<GetEventsItem>) {
        val sorted = upcomingEvents(events, ZonedDateTime.now(ZoneId.systemDefault()))

        if (sorted.isEmpty()) return

        val container = binding.upcomingEventsContainer
        sorted.forEach { container.addEventCard(it) }
        binding.eventsTitle.isVisible = true
        container.isVisible = true
    }

    private fun ViewGroup.addEventCard(event: GetEventsItem) {
        val card = ItemAreaCardBinding.inflate(layoutInflater, this, true)
        val icon = ImageView(requireContext()).apply {
            setImageResource(R.drawable.icon_event)
            imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorSecondary,
                    0,
                )
            )
        }
        card.icon.addView(
            icon,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        card.title.text = event.name
        card.subtitle.text = event.startsAt.format(eventDateFormat)
        card.root.setOnClickListener {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<EventFragment>(R.id.fragmentContainerView, null, event.toBundle())
                addToBackStack(null)
            }
        }
    }

    private data class IssueRow(
        val issue: GetPlaceIssuesItem,
        val place: Place?,
    )

    private suspend fun fetchPlaceIssues(): Pair<List<IssueRow>, Int> {
        val response = api().getPlaceIssues(areaId, PLACE_ISSUES_LIMIT)
        val rows = response.requestedIssues.map { issue ->
            IssueRow(
                issue = issue,
                place = db().place.selectByOsmId(
                    "${issue.elementOsmType}:${issue.elementOsmId}",
                ),
            )
        }

        return rows to response.totalIssues
    }

    private fun renderPlaceIssues(rows: List<IssueRow>, totalIssues: Int) {
        if (rows.isEmpty()) return

        val density = resources.displayMetrics.density
        val container = binding.issuesContainer

        val topMarginDp = if (binding.upcomingEventsContainer.isVisible) 8 else 24
        (binding.issuesHeader.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
            (topMarginDp * density).toInt()

        rows.forEach { container.addIssueCard(it) }

        binding.issuesTitle.text = if (rows.size < totalIssues) {
            getString(R.string.issues_d_of_d, rows.size, totalIssues)
        } else {
            getString(R.string.issues_d, totalIssues)
        }
        binding.issuesHeader.isVisible = true
        container.isVisible = true
    }

    private fun ViewGroup.addIssueCard(row: IssueRow) {
        val card = ItemAreaCardBinding.inflate(layoutInflater, this, true)
        val placeIcon = row.place?.icon?.takeIf { it.isNotBlank() }
        val icon = TextView(requireContext()).apply {
            typeface = iconTypeface
            text = placeIcon ?: ISSUE_FALLBACK_ICON
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24f)
            setTextColor(
                MaterialColors.getColor(
                    this,
                    if (placeIcon != null) {
                        com.google.android.material.R.attr.colorSecondary
                    } else {
                        android.R.attr.colorError
                    },
                    0,
                )
            )
            gravity = Gravity.CENTER
        }
        card.icon.addView(
            icon,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        card.title.isVisible = row.issue.elementName.isNotBlank()
        card.title.text = row.issue.elementName
        card.subtitle.text = issueDescription(row.issue.issueCode)
        card.root.setOnClickListener {
            openInBrowser(
                "https://www.openstreetmap.org/edit?${row.issue.elementOsmType}=${row.issue.elementOsmId}"
                    .toUri(),
            )
        }
    }

    private fun issueDescription(code: String): String {
        val description = describeIssue(code)
        return if (description.formatArg != null) {
            getString(description.resId, description.formatArg)
        } else {
            getString(description.resId)
        }
    }

    companion object {
        private const val PLACE_ISSUES_LIMIT = 50L

        private const val ISSUE_FALLBACK_ICON = "warning"

        private const val JOIN_US_URL = "https://btcmap.org/join-us"

        private const val EXTRA_AUTH_ACTION = "auth-action"
        private const val EXTRA_AREA_ID = "auth-area-id"
        private const val EXTRA_AREA_NAME = "auth-area-name"

        private const val AUTH_ACTION_TOGGLE_SAVED = "toggle-saved"
    }
}
