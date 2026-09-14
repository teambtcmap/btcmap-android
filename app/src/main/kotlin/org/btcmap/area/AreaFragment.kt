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
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import coil3.load
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.GetEventsItem
import org.btcmap.api.GetPlaceIssuesItem
import org.btcmap.api.getArea
import org.btcmap.api.getPlaceIssues
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
import org.btcmap.db.table.place.Place
import org.btcmap.databinding.AreaFragmentBinding
import org.btcmap.saved.toggleSavedArea
import org.btcmap.settings.authorized
import org.btcmap.settings.prefs
import org.btcmap.util.iconTypeface
import org.btcmap.util.openInBrowser
import org.btcmap.util.showError
import org.btcmap.util.rethrowIfCancellation
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AreaFragment : Fragment() {

    private val areaId by lazy {
        requireArguments().getString("area_id")!!
    }

    private val upcomingEvents: List<GetEventsItem> by lazy {
        @Suppress("DEPRECATION")
        val raw = arguments?.getParcelableArrayList<Bundle>("upcoming_events") ?: emptyList()
        raw.map { it.toGetEventsItem() }
    }

    private var _binding: AreaFragmentBinding? = null
    private val binding get() = _binding!!

    private var toolbarContentColor = Color.WHITE

    private var appBarCollapsed = false

    private var areaName = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AreaFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.save -> onSaveClicked()
            }

            true
        }

        initInsets()
        initCollapsingToolbar()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val area = withContext(Dispatchers.IO) {
                    api().getArea(areaId)
                }
                areaName = area.name
                binding.toolbar.title = area.name
                val headerImage = area.iconWide ?: area.icon
                binding.icon.isVisible = headerImage != null
                binding.icon.load(headerImage)
                updateToolbarContentColor()
                binding.description.isVisible = area.description != null
                binding.description.text = area.description
                binding.website.text = (if (areaId == "671") "https://btcmap.org/phuket" else area.websiteUrl)
                    .replace("https://", "")
                    .replace("http://", "")
                    .trimEnd('/')
                updateBookmarkIcon()
                renderUpcomingEvents()
                loadPlaceIssues()
                binding.loading.isVisible = false
                binding.content.isVisible = true
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                binding.loading.isVisible = false
                showLoadError(e)
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
        if (prefs.authorized) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = db().user.select()!!
                    val saved =
                        user.savedAreas.any { it.asJsonObject["id"].asLong == areaId.toLong() }
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
        } else {
            binding.toolbar.menu.findItem(R.id.save).setIcon(R.drawable.icon_bookmark)
        }
    }

    private fun renderUpcomingEvents() {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val sorted = upcomingEvents
            .filter { it.startsAt.isAfter(now) }
            .sortedBy { it.startsAt }

        if (sorted.isEmpty()) return

        val container = binding.upcomingEventsContainer
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm")

        for (event in sorted) {
            val itemBlock = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                val itemParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                itemParams.bottomMargin = (16 * resources.displayMetrics.density).toInt()
                layoutParams = itemParams
                val pad = (16 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                gravity = Gravity.CENTER_VERTICAL
                background = androidx.core.content.ContextCompat.getDrawable(
                    context,
                    R.drawable.event_card_background,
                )
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    openInBrowser(event.website.toString().toUri())
                }
            }

            val iconView = ImageView(requireContext()).apply {
                setImageResource(R.drawable.icon_event)
                val tint = com.google.android.material.color.MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorSecondary,
                    0,
                )
                imageTintList = android.content.res.ColorStateList.valueOf(tint)
                val iconSize = (24 * resources.displayMetrics.density).toInt()
                val iconParams = ViewGroup.MarginLayoutParams(iconSize, iconSize)
                val iconEnd = (12 * resources.displayMetrics.density).toInt()
                iconParams.marginEnd = iconEnd
                layoutParams = iconParams
            }
            itemBlock.addView(iconView)

            val textBlock = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }

            val nameView = TextView(requireContext()).apply {
                text = event.name
                setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline6)
                setPadding(0, 0, 0, 4)
            }
            textBlock.addView(nameView)

            val dateView = TextView(requireContext()).apply {
                text = event.startsAt.format(dateFormatter)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1)
                setPadding(0, 4, 0, 0)
            }
            textBlock.addView(dateView)

            itemBlock.addView(textBlock)

            container.addView(itemBlock)
        }

        binding.eventsTitle.isVisible = true
        binding.upcomingEventsContainer.isVisible = true
    }

    private data class IssueRow(
        val issue: GetPlaceIssuesItem,
        val place: Place?,
    )

    private fun loadPlaceIssues() {
        val id = areaId.toLongOrNull() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (rows, totalIssues) = withContext(Dispatchers.IO) {
                    val response = api().getPlaceIssues(id, PLACE_ISSUES_LIMIT)
                    val rows = response.requestedIssues.map { issue ->
                        IssueRow(
                            issue = issue,
                            place = db().place.selectByOsmId(
                                "${issue.elementOsmType}:${issue.elementOsmId}",
                            ),
                        )
                    }
                    rows to response.totalIssues
                }
                renderPlaceIssues(rows, totalIssues)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                showError(e)
            }
        }
    }

    private fun renderPlaceIssues(rows: List<IssueRow>, totalIssues: Int) {
        if (rows.isEmpty()) return

        val density = resources.displayMetrics.density
        val container = binding.issuesContainer

        for (row in rows) {
            val issue = row.issue
            val itemBlock = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = (16 * density).toInt()
                }
                val pad = (16 * density).toInt()
                setPadding(pad, pad, pad, pad)
                gravity = Gravity.CENTER_VERTICAL
                background = androidx.core.content.ContextCompat.getDrawable(
                    context,
                    R.drawable.event_card_background,
                )
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    openInBrowser(
                        "https://www.openstreetmap.org/edit?${issue.elementOsmType}=${issue.elementOsmId}"
                            .toUri(),
                    )
                }
            }

            val placeIcon = row.place?.icon?.takeIf { it.isNotBlank() }
            val iconView = TextView(requireContext()).apply {
                typeface = iconTypeface
                text = placeIcon ?: "warning"
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24f)
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
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
                val iconSize = (24 * density).toInt()
                layoutParams = ViewGroup.MarginLayoutParams(iconSize, iconSize).apply {
                    marginEnd = (12 * density).toInt()
                }
            }
            itemBlock.addView(iconView)

            val textBlock = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }

            val nameView = TextView(requireContext()).apply {
                isVisible = issue.elementName.isNotBlank()
                text = issue.elementName
                setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline6)
                setPadding(0, 0, 0, 4)
            }
            textBlock.addView(nameView)

            val descriptionView = TextView(requireContext()).apply {
                text = issueDescription(issue.issueCode)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1)
                setPadding(0, 4, 0, 0)
            }
            textBlock.addView(descriptionView)

            itemBlock.addView(textBlock)

            container.addView(itemBlock)
        }

        binding.issuesTitle.text = if (rows.size < totalIssues) {
            getString(R.string.issues_d_of_d, rows.size, totalIssues)
        } else {
            getString(R.string.issues_d, totalIssues)
        }
        binding.issuesTitle.isVisible = true
        binding.issuesContainer.isVisible = true
    }

    private fun issueDescription(code: String): String {
        return when {
            code == "outdated" -> getString(R.string.issue_outdated)
            code == "outdated_soon" -> getString(R.string.issue_outdated_soon)
            code == "not_verified" -> getString(R.string.not_verified)
            code == "missing_icon" -> getString(R.string.issue_missing_icon)
            code.startsWith("invalid_tag_value:") -> getString(
                R.string.issue_invalid_tag_value,
                code.substringAfter("invalid_tag_value:"),
            )
            code.startsWith("misspelled_tag_name:") -> getString(
                R.string.issue_misspelled_tag_name,
                code.substringAfter("misspelled_tag_name:"),
            )
            else -> getString(R.string.issue_unknown)
        }
    }

    private fun Bundle.toGetEventsItem(): GetEventsItem {
        val areaIdRaw = getString("area_id")
        val endsAtRaw = getString("ends_at")
        return GetEventsItem(
            id = getLong("id"),
            areaId = areaIdRaw?.toLongOrNull(),
            lat = getDouble("lat"),
            lon = getDouble("lon"),
            name = getString("name").orEmpty(),
            website = getString("website").orEmpty().toHttpUrlOrNull()
                ?: error("Missing website for event ${getLong("id")}"),
            startsAt = ZonedDateTime.parse(getString("starts_at")!!),
            endsAt = endsAtRaw?.let { ZonedDateTime.parse(it) },
        )
    }

    companion object {
        private const val PLACE_ISSUES_LIMIT = 50L
    }
}
