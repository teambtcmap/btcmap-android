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
import org.btcmap.api.getArea
import org.btcmap.api.getAreaEvents
import org.btcmap.api.getPlaceIssues
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
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
        requireArguments().getString(ARG_AREA_ID)!!
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
            try {
                val area = withContext(Dispatchers.IO) {
                    api().getArea(areaId)
                }
                renderArea(area)
                binding.loading.isVisible = false
                binding.content.isVisible = true
                loadSecondarySections()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                binding.loading.isVisible = false
                showLoadError(e)
            }
        }
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

    private fun loadSecondarySections() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) {
                    api().getAreaEvents(areaId)
                }
                renderUpcomingEvents(events)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }

            try {
                val (rows, totalIssues) = withContext(Dispatchers.IO) {
                    fetchPlaceIssues()
                }
                renderPlaceIssues(rows, totalIssues)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun onSaveClicked() {
        val toggle = {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    toggleSavedArea(areaId.toLong(), areaName)
                    updateBookmarkIcon()
                } catch (e: Throwable) {
                    e.rethrowIfCancellation()
                    showError(e)
                }
            }
            Unit
        }

        if (prefs.authorized) {
            toggle()
        } else {
            showAuthDialog { toggle() }
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
                val saved = withContext(Dispatchers.IO) { isAreaSaved(areaId.toLong()) }
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
        val id = areaId.toLongOrNull() ?: return emptyList<IssueRow>() to 0

        val response = api().getPlaceIssues(id, PLACE_ISSUES_LIMIT)
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
    }
}
